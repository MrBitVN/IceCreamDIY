import SceneKit
import UIKit

final class SoftServeGenerator {
    // Builds the T-shaped dispenser holder with 3 flavor canisters and star nozzle
    static func createDispenserNode() -> SCNNode {
        let dispenserRoot = SCNNode()
        dispenserRoot.name = "dispenserRoot"

        let silverMaterial = SCNMaterial()
        silverMaterial.diffuse.contents = UIColor(red: 0.86, green: 0.88, blue: 0.90, alpha: 1.0)
        silverMaterial.metalness.contents = 0.85
        silverMaterial.roughness.contents = 0.25
        silverMaterial.lightingModel = .physicallyBased

        // Horizontal silver metallic beam
        let beamGeo = SCNCylinder(radius: 0.16, height: 1.45)
        beamGeo.materials = [silverMaterial]
        let beamNode = SCNNode(geometry: beamGeo)
        beamNode.eulerAngles = SCNVector3(0, 0, Float.pi / 2.0)
        beamNode.position = SCNVector3(0, 0, 0)
        dispenserRoot.addChildNode(beamNode)

        // Center star-fluted nozzle pointing downwards
        let nozzleNode = createStarNozzleNode()
        nozzleNode.geometry?.firstMaterial = silverMaterial
        nozzleNode.position = SCNVector3(0, -0.22, 0)
        dispenserRoot.addChildNode(nozzleNode)

        // 3 Flavor Tubes on top of the beam
        let tubeFlavors: [(Flavor, Float)] = [
            (.vanilla, -0.42),
            (.strawberry, 0.0),
            (.chocolate, 0.42)
        ]

        let tubeRadius: CGFloat = 0.11
        let tubeHeight: CGFloat = 0.55

        for (flavor, posX) in tubeFlavors {
            let tubeGeo = SCNCylinder(radius: tubeRadius, height: tubeHeight)
            let mat = SCNMaterial()
            mat.diffuse.contents = flavor.uiColor
            mat.roughness.contents = 0.35
            mat.lightingModel = .physicallyBased
            tubeGeo.materials = [mat]

            let tubeNode = SCNNode(geometry: tubeGeo)
            tubeNode.position = SCNVector3(posX, Float(tubeHeight / 2.0) + 0.12, 0)
            tubeNode.name = "tube_\(flavor.rawValue)"
            dispenserRoot.addChildNode(tubeNode)
        }

        return dispenserRoot
    }

    // Procedural 8-point star nozzle
    static func createStarNozzleNode(teeth: Int = 8, segments: Int = 32) -> SCNNode {
        let node = SCNNode()
        let topRadius: Float = 0.16
        let bottomRadius: Float = 0.09
        let height: Float = 0.32
        let halfHeight = height / 2.0

        var vertices: [SCNVector3] = []
        var normals: [SCNVector3] = []
        var indices: [Int32] = []

        for i in 0...segments {
            let u = Float(i) / Float(segments)
            let angle = u * 2.0 * Float.pi

            let starMod = 1.0 + 0.22 * cos(Float(teeth) * angle)
            let rTop = topRadius * starMod
            let rBot = bottomRadius * starMod

            let cosA = cos(angle)
            let sinA = sin(angle)

            // Top ring
            vertices.append(SCNVector3(rTop * cosA, halfHeight, rTop * sinA))
            normals.append(SCNVector3(cosA, 0.15, sinA))

            // Bottom ring (nozzle tip)
            vertices.append(SCNVector3(rBot * cosA, -halfHeight, rBot * sinA))
            normals.append(SCNVector3(cosA, -0.15, sinA))
        }

        for i in 0..<segments {
            let p1 = Int32(i * 2)
            let p2 = Int32(i * 2 + 1)
            let p3 = Int32((i + 1) * 2)
            let p4 = Int32((i + 1) * 2 + 1)

            indices.append(p1)
            indices.append(p2)
            indices.append(p3)

            indices.append(p3)
            indices.append(p2)
            indices.append(p4)
        }

        let vertexSource = SCNGeometrySource(vertices: vertices)
        let normalSource = SCNGeometrySource(normals: normals)
        let element = SCNGeometryElement(indices: indices, primitiveType: .triangles)

        let geo = SCNGeometry(sources: [vertexSource, normalSource], elements: [element])
        let mat = SCNMaterial()
        mat.diffuse.contents = UIColor(red: 0.88, green: 0.72, blue: 0.52, alpha: 1.0)
        mat.roughness.contents = 0.4
        mat.lightingModel = .physicallyBased
        geo.materials = [mat]

        node.geometry = geo
        return node
    }

    // Dynamic stream cylinder from nozzle to current top of ice cream
    static func createStreamNode(topY: Float, bottomY: Float, color: UIColor) -> SCNNode {
        let height = max(topY - bottomY, 0.02)
        let cylinder = SCNCylinder(radius: 0.065, height: CGFloat(height))
        let mat = SCNMaterial()
        mat.diffuse.contents = color
        mat.roughness.contents = 0.35
        mat.lightingModel = .physicallyBased
        cylinder.materials = [mat]

        let node = SCNNode(geometry: cylinder)
        node.position = SCNVector3(0, bottomY + height / 2.0, 0)
        node.name = "pouringStream"
        return node
    }
}
