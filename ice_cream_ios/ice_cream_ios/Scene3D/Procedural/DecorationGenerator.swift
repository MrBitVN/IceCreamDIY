import Foundation
import SceneKit
import UIKit

final class DecorationGenerator {

    static func createCherryNode() -> SCNNode {
        let rootNode = SCNNode()
        rootNode.name = "cherryRoot"

        // Glossy red sphere
        let sphereGeo = SCNSphere(radius: 0.12)
        let redMat = SCNMaterial()
        redMat.diffuse.contents = UIColor(red: 0.92, green: 0.08, blue: 0.16, alpha: 1.0)
        redMat.roughness.contents = 0.08
        redMat.lightingModel = .physicallyBased
        sphereGeo.materials = [redMat]

        let sphereNode = SCNNode(geometry: sphereGeo)
        rootNode.addChildNode(sphereNode)

        // Curved stem
        let stemGeo = SCNCylinder(radius: 0.015, height: 0.28)
        let stemMat = SCNMaterial()
        stemMat.diffuse.contents = UIColor(red: 0.35, green: 0.22, blue: 0.12, alpha: 1.0)
        stemGeo.materials = [stemMat]

        let stemNode = SCNNode(geometry: stemGeo)
        stemNode.position = SCNVector3(0.04, 0.16, 0)
        stemNode.eulerAngles = SCNVector3(0, 0, -Float.pi / 8.0)
        rootNode.addChildNode(stemNode)

        return rootNode
    }

    static func createSprinkleNodes(count: Int, totalProgress: Float) -> [SCNNode] {
        var nodes: [SCNNode] = []
        let colors: [UIColor] = [
            UIColor(red: 1.0, green: 0.45, blue: 0.70, alpha: 1.0), // Pastel pink
            UIColor(red: 0.40, green: 0.85, blue: 0.95, alpha: 1.0), // Pastel cyan
            UIColor(red: 1.0, green: 0.90, blue: 0.35, alpha: 1.0), // Pastel yellow
            UIColor(red: 0.55, green: 0.32, blue: 0.18, alpha: 1.0), // Chocolate brown
            UIColor.white
        ]

        let totalTurns: Float = 4.2
        let sprinkleGeo = SCNCapsule(capRadius: 0.02, height: 0.075)

        for i in 0..<count {
            let t = 0.12 + (Float(i) / Float(count)) * (totalProgress * 0.82)
            let angle = t * totalTurns * 2.0 * Float.pi + (Float(i * 137) * Float.pi / 180.0)

            let helixRadius = (0.38 * (1.0 - 0.76 * t)) + 0.15
            let px = helixRadius * cos(angle)
            let pz = helixRadius * sin(angle)
            let py = t * 1.55 + 0.05

            let mat = SCNMaterial()
            mat.diffuse.contents = colors[i % colors.count]
            mat.roughness.contents = 0.3

            let node = SCNNode(geometry: sprinkleGeo)
            node.geometry?.materials = [mat]
            node.position = SCNVector3(px, py, pz)
            node.eulerAngles = SCNVector3(Float(i * 45), Float(i * 73), Float(i * 31))
            nodes.append(node)
        }
        return nodes
    }
}
