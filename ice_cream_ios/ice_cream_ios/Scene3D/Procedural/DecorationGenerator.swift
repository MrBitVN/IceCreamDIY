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

        // Curved low-poly stem along a quadratic Bezier curve
        let stemMat = SCNMaterial()
        stemMat.diffuse.contents = UIColor(red: 0.35, green: 0.22, blue: 0.12, alpha: 1.0)
        stemMat.roughness.contents = 0.6

        let segments = 6
        var prevPt = SCNVector3(0, 0.09, 0)
        for i in 1...segments {
            let t = Float(i) / Float(segments)
            let omt = 1.0 - t
            let pt = SCNVector3(
                omt * omt * 0.0 + 2.0 * omt * t * 0.015 + t * t * 0.11,
                omt * omt * 0.09 + 2.0 * omt * t * 0.22 + t * t * 0.29,
                omt * omt * 0.0 + 2.0 * omt * t * 0.0 + t * t * 0.02
            )
            let dx = pt.x - prevPt.x
            let dy = pt.y - prevPt.y
            let dz = pt.z - prevPt.z
            let segLen = sqrt(dx * dx + dy * dy + dz * dz)

            let segGeo = SCNCylinder(radius: 0.015, height: CGFloat(segLen))
            segGeo.materials = [stemMat]
            let segNode = SCNNode(geometry: segGeo)
            segNode.position = SCNVector3(
                (prevPt.x + pt.x) * 0.5,
                (prevPt.y + pt.y) * 0.5,
                (prevPt.z + pt.z) * 0.5
            )
            segNode.eulerAngles = SCNVector3(atan2(dz, dy), 0, -atan2(dx, dy))
            rootNode.addChildNode(segNode)
            prevPt = pt
        }

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
