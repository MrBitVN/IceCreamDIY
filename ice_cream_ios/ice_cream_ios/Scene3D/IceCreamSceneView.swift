import SwiftUI
import SceneKit

struct IceCreamSceneView: UIViewRepresentable {
    let activeFlavors: Set<Flavor>
    let isRotating: Bool
    let pourProgress: Float
    let extraCones: Int
    let sprinklesCount: Int
    let hasCherry: Bool
    let level: Int
    let gamePhase: GamePhase
    let customerVariant: Int
    let stars: Int

    init(
        activeFlavors: Set<Flavor>,
        isRotating: Bool,
        pourProgress: Float,
        extraCones: Int = 1,
        sprinklesCount: Int = 0,
        hasCherry: Bool = false,
        level: Int = 1,
        gamePhase: GamePhase = .pouring,
        customerVariant: Int = 0,
        stars: Int = 0
    ) {
        self.activeFlavors = activeFlavors
        self.isRotating = isRotating
        self.pourProgress = pourProgress
        self.extraCones = extraCones
        self.sprinklesCount = sprinklesCount
        self.hasCherry = hasCherry
        self.level = level
        self.gamePhase = gamePhase
        self.customerVariant = customerVariant
        self.stars = stars
    }

    func makeCoordinator() -> Coordinator {
        Coordinator()
    }

    func makeUIView(context: Context) -> SCNView {
        let scnView = SCNView()
        let scene = SCNScene()
        scnView.scene = scene
        scnView.backgroundColor = UIColor.clear
        scnView.autoenablesDefaultLighting = false
        scnView.antialiasingMode = .multisampling4X
        scnView.isUserInteractionEnabled = false

        // Camera setup
        let cameraNode = SCNNode()
        let camera = SCNCamera()
        camera.fieldOfView = 48
        camera.zNear = 0.1
        camera.zFar = 50
        cameraNode.camera = camera
        cameraNode.position = SCNVector3(0, 0.45, 3.65)
        cameraNode.look(at: SCNVector3(0, -0.15, 0))
        scene.rootNode.addChildNode(cameraNode)

        // Lighting
        SceneLighting.setupLighting(for: scene)

        // T-shaped Dispenser with Star Nozzle
        let dispenserNode = SoftServeGenerator.createDispenserNode()
        dispenserNode.position = SCNVector3(0, 1.35, 0)
        dispenserNode.name = "dispenser"
        scene.rootNode.addChildNode(dispenserNode)

        // Waffle Cone Root Node
        let coneNode = ConeGenerator.createWaffleConeNode()
        coneNode.position = SCNVector3(0, -0.62, 0)
        coneNode.name = "coneRoot"
        scene.rootNode.addChildNode(coneNode)

        // Idle slow rotation
        let rotationAction = SCNAction.rotateBy(x: 0, y: CGFloat(Float.pi * 2.0), z: 0, duration: 10.0)
        let repeatAction = SCNAction.repeatForever(rotationAction)
        coneNode.runAction(repeatAction, forKey: "idleRotation")

        // Dynamic Ice Cream Swirl Node (sits on cone rim)
        let swirlNode = SCNNode()
        swirlNode.position = SCNVector3(0, 0.48, 0)
        swirlNode.name = "swirlMesh"
        coneNode.addChildNode(swirlNode)

        // Stream Node (between nozzle and cone)
        let streamNode = SoftServeGenerator.createStreamNode(
            topY: 1.05,
            bottomY: 0.40,
            color: UIColor.white
        )
        streamNode.isHidden = true
        scene.rootNode.addChildNode(streamNode)

        context.coordinator.coneNode = coneNode
        context.coordinator.swirlNode = swirlNode
        context.coordinator.streamNode = streamNode
        context.coordinator.dispenserNode = dispenserNode
        context.coordinator.scene = scene

        return scnView
    }

    func updateUIView(_ uiView: SCNView, context: Context) {
        context.coordinator.update(
            activeFlavors: Array(activeFlavors),
            pourProgress: pourProgress,
            isRotating: isRotating,
            extraCones: extraCones,
            sprinklesCount: sprinklesCount,
            hasCherry: hasCherry,
            level: level,
            gamePhase: gamePhase,
            customerVariant: customerVariant,
            stars: stars
        )
    }

    class Coordinator {
        var coneNode: SCNNode?
        var swirlNode: SCNNode?
        var streamNode: SCNNode?
        var dispenserNode: SCNNode?
        var stackedConeNode: SCNNode?
        var cherryNode: SCNNode?
        var sprinkleNodes: [SCNNode] = []
        var shopEnvironmentNode: SCNNode?
        var customerNode: SCNNode?
        var dollarBillsNode: SCNNode?
        var scene: SCNScene?

        let meshExtruder = MeshExtruder(ringSegments: 16, maxRings: 180, lobes: 6)
        var lastExtrudedT: Float = 0.0
        var lastSprinklesCount: Int = 0
        var hasAddedShop = false
        var currentReactionPhase: GamePhase?

        func update(
            activeFlavors: [Flavor],
            pourProgress: Float,
            isRotating: Bool,
            extraCones: Int,
            sprinklesCount: Int,
            hasCherry: Bool,
            level: Int,
            gamePhase: GamePhase,
            customerVariant: Int,
            stars: Int
        ) {
            guard let cone = coneNode, let swirl = swirlNode, let stream = streamNode, let dispenser = dispenserNode, let scene = scene else { return }

            // Shop Interior (level >= 5)
            if level >= 5 && !hasAddedShop {
                let shop = ShopEnvironmentGenerator.createShopEnvironmentNode()
                scene.rootNode.addChildNode(shop)
                shopEnvironmentNode = shop
                hasAddedShop = true
            }

            // Reset if restarted
            if pourProgress < 0.001 {
                if meshExtruder.currentRingCount > 0 {
                    meshExtruder.reset()
                    lastExtrudedT = 0.0
                    swirl.geometry = nil
                    sprinkleNodes.forEach { $0.removeFromParentNode() }
                    sprinkleNodes.removeAll()
                    cherryNode?.removeFromParentNode()
                    cherryNode = nil
                    customerNode?.removeFromParentNode()
                    customerNode = nil
                    dollarBillsNode?.removeFromParentNode()
                    dollarBillsNode = nil
                    dispenser.isHidden = false
                    cone.removeAction(forKey: "idleRotation")
                    let rotationAction = SCNAction.rotateBy(x: 0, y: CGFloat(Float.pi * 2.0), z: 0, duration: 10.0)
                    cone.runAction(SCNAction.repeatForever(rotationAction), forKey: "idleRotation")
                }
            }

            // Customer reaction handling
            if gamePhase == .customerReaction {
                dispenser.isHidden = true
                cone.removeAction(forKey: "idleRotation")

                if customerNode == nil {
                    let cust = CustomerGenerator.createCustomerNode(variantIndex: customerVariant, isThumbsUp: stars == 3)
                    cust.position = SCNVector3(0, -0.15, -0.55)
                    scene.rootNode.addChildNode(cust)
                    customerNode = cust

                    let dollars = CustomerGenerator.createDollarBillsNode(count: 16)
                    scene.rootNode.addChildNode(dollars)
                    dollarBillsNode = dollars
                }
            } else {
                dispenser.isHidden = false
                customerNode?.removeFromParentNode()
                customerNode = nil
                dollarBillsNode?.removeFromParentNode()
                dollarBillsNode = nil
            }

            // Lower cone smoothly as ice cream dâng cao
            let baseConeY: Float = -0.62
            let currentConeY = baseConeY - (pourProgress * 0.70)
            cone.position.y = currentConeY

            // Stacked extra cone
            if extraCones >= 2 && stackedConeNode == nil {
                let stacked = ConeGenerator.createWaffleConeNode()
                stacked.position = SCNVector3(0, -0.45, 0)
                cone.addChildNode(stacked)
                stackedConeNode = stacked
            }

            // Extrude rings dynamically
            let stepT: Float = 0.008
            var updated = false

            while (lastExtrudedT + stepT <= pourProgress && meshExtruder.currentRingCount < meshExtruder.maxRings) {
                lastExtrudedT += stepT
                meshExtruder.appendRing(t: lastExtrudedT, activeFlavors: activeFlavors)
                updated = true
            }

            if updated {
                swirl.geometry = meshExtruder.buildGeometry()
            }

            // Dynamic Pouring Stream connecting nozzle tip (Y = 1.05) to current ice cream peak
            if !activeFlavors.isEmpty && pourProgress < 1.0 && gamePhase != .customerReaction {
                stream.isHidden = false
                let primaryColor = activeFlavors.first?.uiColor ?? UIColor.white
                stream.geometry?.firstMaterial?.diffuse.contents = primaryColor

                let nozzleTipY: Float = 1.05
                let peakY = currentConeY + 0.48 + (pourProgress * 1.55)
                let streamHeight = max(nozzleTipY - peakY, 0.04)
                let streamCenterY = nozzleTipY - (streamHeight / 2.0)

                stream.position = SCNVector3(0, streamCenterY, 0)
                let baseHeight: Float = 0.65
                stream.scale = SCNVector3(1.0, streamHeight / baseHeight, 1.0)
            } else {
                stream.isHidden = true
            }

            // Sprinkles
            if sprinklesCount > 0 && sprinklesCount != lastSprinklesCount {
                sprinkleNodes.forEach { $0.removeFromParentNode() }
                sprinkleNodes = DecorationGenerator.createSprinkleNodes(count: sprinklesCount, totalProgress: pourProgress)
                sprinkleNodes.forEach { swirl.addChildNode($0) }
                lastSprinklesCount = sprinklesCount
            }

            // Cherry
            if hasCherry && cherryNode == nil {
                let cNode = DecorationGenerator.createCherryNode()
                let peakY = Float(pourProgress * 1.55) + 0.06
                cNode.position = SCNVector3(0, peakY, 0)
                swirl.addChildNode(cNode)
                cherryNode = cNode
            }
        }
    }
}
