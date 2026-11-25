import SwiftUI
import SceneKit

struct HomeCustomerSceneView: UIViewRepresentable {
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
        camera.fieldOfView = 42
        camera.zNear = 0.1
        camera.zFar = 50
        cameraNode.camera = camera
        cameraNode.position = SCNVector3(0, 0.25, 3.2)
        cameraNode.look(at: SCNVector3(0, 0.10, 0))
        scene.rootNode.addChildNode(cameraNode)

        // Lighting
        SceneLighting.setupLighting(for: scene)

        // Soft Shadow Floor
        let shadowGeo = SCNCylinder(radius: 0.55, height: 0.01)
        shadowGeo.firstMaterial?.diffuse.contents = UIColor(red: 0.6, green: 0.25, blue: 0.35, alpha: 0.25)
        let shadowNode = SCNNode(geometry: shadowGeo)
        shadowNode.position = SCNVector3(0, -0.65, 0)
        scene.rootNode.addChildNode(shadowNode)

        // Low-Poly Customer (Variant 0: Cool Girl with mint top)
        let customerNode = CustomerGenerator.createCustomerNode(variantIndex: 0, isThumbsUp: false)
        customerNode.position = SCNVector3(0, 0, 0)
        scene.rootNode.addChildNode(customerNode)

        // Find or animate the waving right arm
        // Add waving motion
        let waveUp = SCNAction.rotateBy(x: 0, y: 0, z: CGFloat(Float.pi / 7.0), duration: 0.35)
        waveUp.timingMode = .easeInEaseOut
        let waveDown = waveUp.reversed()
        let waveSeq = SCNAction.sequence([waveUp, waveDown])
        customerNode.runAction(SCNAction.repeatForever(waveSeq))

        return scnView
    }

    func updateUIView(_ uiView: SCNView, context: Context) {
    }
}
