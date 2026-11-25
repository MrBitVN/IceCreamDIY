import SceneKit
import UIKit

final class SceneLighting {
    static func setupLighting(for scene: SCNScene) {
        // Key Directional Light (Warm soft sunlight)
        let keyLightNode = SCNNode()
        let keyLight = SCNLight()
        keyLight.type = .directional
        keyLight.intensity = 1100
        keyLight.color = UIColor(red: 1.0, green: 0.98, blue: 0.95, alpha: 1.0)
        keyLight.castsShadow = true
        keyLight.shadowMode = .deferred
        keyLight.shadowRadius = 8.0
        keyLight.shadowSampleCount = 16
        keyLight.shadowColor = UIColor(red: 0.85, green: 0.45, blue: 0.55, alpha: 0.35)
        keyLightNode.light = keyLight
        keyLightNode.position = SCNVector3(2.5, 5.0, 3.5)
        keyLightNode.look(at: SCNVector3(0, 0, 0))
        scene.rootNode.addChildNode(keyLightNode)

        // Soft Fill Light (Pastel pink ambiance)
        let fillLightNode = SCNNode()
        let fillLight = SCNLight()
        fillLight.type = .ambient
        fillLight.intensity = 550
        fillLight.color = UIColor(red: 1.0, green: 0.88, blue: 0.92, alpha: 1.0)
        fillLightNode.light = fillLight
        scene.rootNode.addChildNode(fillLightNode)

        // Back Rim Light
        let rimLightNode = SCNNode()
        let rimLight = SCNLight()
        rimLight.type = .directional
        rimLight.intensity = 400
        rimLight.color = UIColor(white: 1.0, alpha: 1.0)
        rimLightNode.light = rimLight
        rimLightNode.position = SCNVector3(-2.0, 3.0, -3.0)
        rimLightNode.look(at: SCNVector3(0, 0, 0))
        scene.rootNode.addChildNode(rimLightNode)

        // Darker Pink Floor Plane matching Android and reference images
        let floorGeo = SCNPlane(width: 14, height: 14)
        let floorMat = SCNMaterial()
        floorMat.diffuse.contents = UIColor(red: 0.91, green: 0.28, blue: 0.45, alpha: 1.0)
        floorMat.roughness.contents = 0.8
        floorMat.lightingModel = .physicallyBased
        floorGeo.materials = [floorMat]

        let floorNode = SCNNode(geometry: floorGeo)
        floorNode.eulerAngles = SCNVector3(-Float.pi / 2.0, 0, 0)
        floorNode.position = SCNVector3(0, -1.16, 0)
        floorNode.name = "floorPlane"
        scene.rootNode.addChildNode(floorNode)

        // Soft Shadow Disk under the waffle cone
        let shadowGeo = SCNCylinder(radius: 0.45, height: 0.005)
        let shadowMat = SCNMaterial()
        shadowMat.diffuse.contents = UIColor(red: 0.65, green: 0.15, blue: 0.30, alpha: 0.45)
        shadowMat.lightingModel = .constant
        shadowGeo.materials = [shadowMat]

        let shadowNode = SCNNode(geometry: shadowGeo)
        shadowNode.position = SCNVector3(0, -1.155, 0)
        shadowNode.name = "shadowDisk"
        scene.rootNode.addChildNode(shadowNode)
    }
}
