import SwiftUI
import SceneKit
import UIKit

// MARK: - Slicing Minigame SceneKit View

struct SlicingMinigameSceneView: UIViewRepresentable {
    let onKnifeSlice: (MinigameIngredient, CGFloat, CGFloat) -> Void
    let onSwipeTrail: (CGFloat, CGFloat) -> Void
    let onIngredientSorted: (MinigameIngredient, CGFloat, CGFloat) -> Void
    let onWrongCompartment: () -> Void

    func makeCoordinator() -> Coordinator {
        Coordinator(self)
    }

    func makeUIView(context: Context) -> SCNView {
        let scnView = SCNView()
        let scene = SCNScene()
        scnView.scene = scene
        scnView.backgroundColor = UIColor.clear
        scnView.autoenablesDefaultLighting = false
        scnView.antialiasingMode = .multisampling4X
        scnView.isUserInteractionEnabled = true

        context.coordinator.setupScene(scnView: scnView, scene: scene)

        let panGesture = UIPanGestureRecognizer(
            target: context.coordinator,
            action: #selector(Coordinator.handlePan(_:))
        )
        scnView.addGestureRecognizer(panGesture)

        return scnView
    }

    func updateUIView(_ uiView: SCNView, context: Context) {
        // Dynamic scene updates if needed
    }

    final class Coordinator: NSObject {
        let parent: SlicingMinigameSceneView
        weak var scnView: SCNView?
        weak var scene: SCNScene?

        private var knifeNode: SCNNode?
        private var wholeIngredientNodes: [MinigameIngredient: SCNNode] = [:]
        private var slicedIngredientNodes: [MinigameIngredient: SCNNode] = [:]
        private var compartmentFloorNodes: [SCNNode] = []
        private var slicedIngredients: Set<MinigameIngredient> = []
        private var sortedIngredients: Set<MinigameIngredient> = []

        private var lastPanPos: CGPoint = .zero

        init(_ parent: SlicingMinigameSceneView) {
            self.parent = parent
        }

        func setupScene(scnView: SCNView, scene: SCNScene) {
            self.scnView = scnView
            self.scene = scene

            // Camera looking slightly down at tabletop
            let cameraNode = SCNNode()
            let camera = SCNCamera()
            camera.fieldOfView = 50
            camera.zNear = 0.1
            camera.zFar = 50
            cameraNode.camera = camera
            cameraNode.position = SCNVector3(0, 3.8, 3.2)
            cameraNode.eulerAngles = SCNVector3(-0.92, 0, 0)
            scene.rootNode.addChildNode(cameraNode)

            // Lighting
            let ambientLightNode = SCNNode()
            let ambientLight = SCNLight()
            ambientLight.type = .ambient
            ambientLight.color = UIColor(white: 0.75, alpha: 1.0)
            ambientLightNode.light = ambientLight
            scene.rootNode.addChildNode(ambientLightNode)

            let dirLightNode = SCNNode()
            let dirLight = SCNLight()
            dirLight.type = .directional
            dirLight.color = UIColor(white: 0.85, alpha: 1.0)
            dirLight.castsShadow = true
            dirLight.shadowMode = .deferred
            dirLight.shadowColor = UIColor.black.withAlphaComponent(0.20)
            dirLightNode.light = dirLight
            dirLightNode.position = SCNVector3(1.5, 5.0, 2.5)
            dirLightNode.eulerAngles = SCNVector3(-1.0, 0.45, 0)
            scene.rootNode.addChildNode(dirLightNode)

            // Pink Floor plane
            let floorGeo = SCNFloor()
            floorGeo.reflectivity = 0.05
            let floorMat = SCNMaterial()
            floorMat.diffuse.contents = UIColor(red: 0.95, green: 0.65, blue: 0.75, alpha: 1.0)
            floorMat.roughness.contents = 0.85
            floorGeo.materials = [floorMat]
            let floorNode = SCNNode(geometry: floorGeo)
            floorNode.position = SCNVector3(0, -0.05, 0)
            scene.rootNode.addChildNode(floorNode)

            // 1. Procedural Pink Cutting Board
            let boardNode = MinigameProceduralGenerator.createPinkCuttingBoardNode()
            boardNode.position = SCNVector3(0, 0, -0.78)
            scene.rootNode.addChildNode(boardNode)

            // 2. Procedural 3-Compartment Wooden Box
            let boxNode = MinigameProceduralGenerator.createSortingBoxNode(compartmentFloorNodes: &compartmentFloorNodes)
            boxNode.position = SCNVector3(0, 0, 0.82)
            scene.rootNode.addChildNode(boxNode)

            // 3. Whole Ingredients on cutting board
            for ing in MinigameIngredient.allCases {
                let wholeNode = MinigameProceduralGenerator.createWholeIngredientNode(for: ing)
                wholeNode.position = SCNVector3(ing.boardX, 0.09, ing.boardZ)
                scene.rootNode.addChildNode(wholeNode)
                wholeIngredientNodes[ing] = wholeNode
            }

            // 4. Chef Knife Node
            let knife = MinigameProceduralGenerator.createChefKnifeNode()
            knife.position = SCNVector3(0, 0.45, -0.78)
            knife.eulerAngles = SCNVector3(0, 0, -0.15)
            scene.rootNode.addChildNode(knife)
            self.knifeNode = knife
        }

        @objc func handlePan(_ gesture: UIPanGestureRecognizer) {
            guard let view = scnView else { return }
            let location = gesture.location(in: view)

            switch gesture.state {
            case .began:
                lastPanPos = location
                parent.onSwipeTrail(location.x, location.y)
                updateKnifePosition(screenPoint: location, view: view)

            case .changed:
                parent.onSwipeTrail(location.x, location.y)
                updateKnifePosition(screenPoint: location, view: view)
                checkKnifeIntersection(from: lastPanPos, to: location, view: view)
                lastPanPos = location

            case .ended, .cancelled:
                // Return knife to resting height
                if let knife = knifeNode {
                    SCNTransaction.begin()
                    SCNTransaction.animationDuration = 0.25
                    knife.position = SCNVector3(knife.position.x, 0.45, knife.position.z)
                    SCNTransaction.commit()
                }

            default:
                break
            }
        }

        private func updateKnifePosition(screenPoint: CGPoint, view: SCNView) {
            guard let knife = knifeNode else { return }
            let worldPos = unprojectToTabletop(screenPoint: screenPoint, view: view, tableY: 0.12)

            knife.position = SCNVector3(worldPos.x, 0.18, worldPos.z)

            let dx = Float(screenPoint.x - lastPanPos.x)
            let dy = Float(screenPoint.y - lastPanPos.y)
            let angle = atan2(dy, dx)
            knife.eulerAngles = SCNVector3(-0.15, -angle + Float.pi / 2.0, 0.22)
        }

        private func checkKnifeIntersection(from p0: CGPoint, to p1: CGPoint, view: SCNView) {
            let world0 = unprojectToTabletop(screenPoint: p0, view: view, tableY: 0.08)
            let world1 = unprojectToTabletop(screenPoint: p1, view: view, tableY: 0.08)

            for ing in MinigameIngredient.allCases {
                if slicedIngredients.contains(ing) { continue }

                let ingPos = SCNVector3(ing.boardX, 0.09, ing.boardZ)
                let d0 = hypot(world0.x - ingPos.x, world0.z - ingPos.z)
                let d1 = hypot(world1.x - ingPos.x, world1.z - ingPos.z)

                if d0 < 0.36 || d1 < 0.36 {
                    sliceIngredient(ing, screenPoint: p1)
                }
            }
        }

        private func sliceIngredient(_ ingredient: MinigameIngredient, screenPoint: CGPoint) {
            guard !slicedIngredients.contains(ingredient) else { return }
            slicedIngredients.insert(ingredient)

            parent.onKnifeSlice(ingredient, screenPoint.x, screenPoint.y)

            // Hide whole ingredient node
            if let wholeNode = wholeIngredientNodes[ingredient] {
                SCNTransaction.begin()
                SCNTransaction.animationDuration = 0.12
                wholeNode.scale = SCNVector3(0.01, 0.01, 0.01)
                SCNTransaction.commit()
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.15) {
                    wholeNode.removeFromParentNode()
                }
            }

            // Create sliced pieces node and animate flying arc to 3-compartment box
            guard let scene = self.scene else { return }
            let slicedNode = MinigameProceduralGenerator.createSlicedIngredientNode(for: ingredient)
            let startPos = SCNVector3(ingredient.boardX, 0.14, ingredient.boardZ)
            let targetPos = SCNVector3(ingredient.targetCenterX, 0.08, ingredient.targetCenterZ)
            slicedNode.position = startPos
            scene.rootNode.addChildNode(slicedNode)
            slicedIngredientNodes[ingredient] = slicedNode

            // Parabolic Flight Arc Animation + Elastic Bounce on Landing
            let duration: TimeInterval = 0.65
            let arcAction = SCNAction.customAction(duration: duration) { node, elapsedTime in
                let t = Float(elapsedTime / CGFloat(duration))
                let x = startPos.x + (targetPos.x - startPos.x) * t
                let z = startPos.z + (targetPos.z - startPos.z) * t
                let heightArc: Float = 4.0 * 0.95 * t * (1.0 - t)
                let y = startPos.y + (targetPos.y - startPos.y) * t + heightArc

                node.position = SCNVector3(x, y, z)
                node.eulerAngles = SCNVector3(t * Float.pi * 2, t * Float.pi * 3, 0)
            }

            let bounceDuration: TimeInterval = 0.55
            let bounceAction = SCNAction.customAction(duration: bounceDuration) { node, elapsedTime in
                let elapsedSec = Float(elapsedTime)
                let s = MinigameEngine.elasticBounceScale(elapsedSec: elapsedSec)
                node.scale = SCNVector3(s, s, s)
            }

            let sequence = SCNAction.sequence([
                arcAction,
                bounceAction,
                SCNAction.run { [weak self] _ in
                    guard let self = self else { return }
                    self.sortedIngredients.insert(ingredient)
                    self.parent.onIngredientSorted(ingredient, screenPoint.x, screenPoint.y)
                }
            ])

            slicedNode.runAction(sequence)
        }

        private func unprojectToTabletop(screenPoint: CGPoint, view: SCNView, tableY: Float) -> SCNVector3 {
            let pNear = view.unprojectPoint(SCNVector3(Float(screenPoint.x), Float(screenPoint.y), 0.0))
            let pFar = view.unprojectPoint(SCNVector3(Float(screenPoint.x), Float(screenPoint.y), 1.0))

            let dy = pFar.y - pNear.y
            if abs(dy) < 0.0001 { return SCNVector3(pNear.x, tableY, pNear.z) }

            let t = (tableY - pNear.y) / dy
            let x = pNear.x + t * (pFar.x - pNear.x)
            let z = pNear.z + t * (pFar.z - pNear.z)
            return SCNVector3(x, tableY, z)
        }
    }
}

// MARK: - Blender Minigame SceneKit View

struct BlenderMinigameSceneView: UIViewRepresentable {
    let fillProgress: Float

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

        context.coordinator.setupScene(scnView: scnView, scene: scene)

        return scnView
    }

    func updateUIView(_ uiView: SCNView, context: Context) {
        context.coordinator.updateFillProgress(fillProgress)
    }

    final class Coordinator: NSObject {
        private var liquidNodes: [SCNNode] = []
        private var bladeNodes: [SCNNode] = []
        private var bubbleLayers: [SCNNode] = []

        func setupScene(scnView: SCNView, scene: SCNScene) {
            // Camera
            let cameraNode = SCNNode()
            let camera = SCNCamera()
            camera.fieldOfView = 48
            camera.zNear = 0.1
            camera.zFar = 50
            cameraNode.camera = camera
            cameraNode.position = SCNVector3(0, 0.45, 3.4)
            scene.rootNode.addChildNode(cameraNode)

            // Lights
            let ambient = SCNNode()
            let aLight = SCNLight()
            aLight.type = .ambient
            aLight.color = UIColor(white: 0.72, alpha: 1.0)
            ambient.light = aLight
            scene.rootNode.addChildNode(ambient)

            let dir = SCNNode()
            let dLight = SCNLight()
            dLight.type = .directional
            dLight.color = UIColor(white: 0.88, alpha: 1.0)
            dir.light = dLight
            dir.position = SCNVector3(2.0, 4.0, 3.0)
            dir.eulerAngles = SCNVector3(-0.85, 0.45, 0)
            scene.rootNode.addChildNode(dir)

            // 3 Blender Cups: Yellow (Mango), Purple (Berry), Green (Mint)
            let cupConfigs: [(x: Float, color: UIColor, bubbleColor: UIColor)] = [
                (-0.85, UIColor(red: 1.0, green: 0.84, blue: 0.14, alpha: 0.92), UIColor(red: 1.0, green: 0.95, blue: 0.45, alpha: 0.85)),
                (0.0,   UIColor(red: 0.61, green: 0.32, blue: 0.88, alpha: 0.92), UIColor(red: 0.78, green: 0.52, blue: 0.98, alpha: 0.85)),
                (0.85,  UIColor(red: 0.34, green: 0.80, blue: 0.36, alpha: 0.92), UIColor(red: 0.58, green: 0.95, blue: 0.59, alpha: 0.85))
            ]

            for cfg in cupConfigs {
                let cupRoot = SCNNode()
                cupRoot.position = SCNVector3(cfg.x, -0.65, 0)

                // Glass cup geometry (transparent tapered cylinder)
                let glassGeo = MinigameProceduralGenerator.createBottomAnchoredTaperedGeometry(
                    bottomRadius: 0.28,
                    topRadius: 0.38,
                    height: 1.15,
                    segments: 24,
                    color: UIColor(white: 1.0, alpha: 0.28),
                    alpha: 0.28
                )
                let glassNode = SCNNode(geometry: glassGeo)
                cupRoot.addChildNode(glassNode)

                // Liquid node (scaled vertically from bottom anchor)
                let liquidGeo = MinigameProceduralGenerator.createBottomAnchoredTaperedGeometry(
                    bottomRadius: 0.26,
                    topRadius: 0.36,
                    height: 1.05,
                    segments: 24,
                    color: cfg.color,
                    alpha: 0.92
                )
                let liquidNode = SCNNode(geometry: liquidGeo)
                liquidNode.scale = SCNVector3(1.0, 0.05, 1.0)
                cupRoot.addChildNode(liquidNode)
                liquidNodes.append(liquidNode)

                // Frothy bubble layer on top
                let bubbleGeo = SCNCylinder(radius: 0.34, height: 0.04)
                let bubbleMat = SCNMaterial()
                bubbleMat.diffuse.contents = cfg.bubbleColor
                bubbleMat.roughness.contents = 0.5
                bubbleGeo.materials = [bubbleMat]
                let bubbleNode = SCNNode(geometry: bubbleGeo)
                bubbleNode.position = SCNVector3(0, 0.05, 0)
                cupRoot.addChildNode(bubbleNode)
                bubbleLayers.append(bubbleNode)

                // Agitator blades at base
                let bladeGeo = SCNBox(width: 0.36, height: 0.02, length: 0.06, chamferRadius: 0.005)
                let bladeMat = SCNMaterial()
                bladeMat.diffuse.contents = UIColor(white: 0.88, alpha: 1.0)
                bladeMat.metalness.contents = 0.9
                bladeGeo.materials = [bladeMat]
                let bladeNode = SCNNode(geometry: bladeGeo)
                bladeNode.position = SCNVector3(0, 0.04, 0)
                cupRoot.addChildNode(bladeNode)
                bladeNodes.append(bladeNode)

                // Continuous rotation of agitator
                let spin = SCNAction.repeatForever(SCNAction.rotateBy(x: 0, y: CGFloat(Float.pi * 2), z: 0, duration: 0.35))
                bladeNode.runAction(spin)

                scene.rootNode.addChildNode(cupRoot)
            }
        }

        func updateFillProgress(_ progress: Float) {
            let clamped = max(0.05, min(1.0, progress))
            for (idx, liquid) in liquidNodes.enumerated() {
                liquid.scale = SCNVector3(1.0, clamped, 1.0)
                if idx < bubbleLayers.count {
                    bubbleLayers[idx].position = SCNVector3(0, clamped * 1.05, 0)
                }
            }
        }
    }
}
