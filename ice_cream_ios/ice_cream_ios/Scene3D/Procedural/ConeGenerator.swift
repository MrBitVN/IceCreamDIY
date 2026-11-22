import SceneKit
import UIKit

final class ConeGenerator {
    static func createWaffleConeNode() -> SCNNode {
        let rootNode = SCNNode()
        rootNode.name = "coneRoot"

        // Dimensions matching reference images
        let topRadius: CGFloat = 0.52
        let bottomRadius: CGFloat = 0.28
        let height: CGFloat = 0.95
        
        let coneGeometry = SCNCylinder(radius: topRadius, height: height)
        // Approximate a tapered cone via a procedural cone geometry or truncated cylinder
        let coneNode = SCNNode(geometry: createTruncatedConeGeometry(topRadius: topRadius, bottomRadius: bottomRadius, height: height, radialSegments: 36))
        coneNode.name = "coneBody"
        
        // Material with waffle texture
        let material = SCNMaterial()
        material.diffuse.contents = generateWaffleTexture()
        material.roughness.contents = 0.6
        material.lightingModel = .physicallyBased
        coneNode.geometry?.materials = [material]

        // Top rim of cone (thick waffle lip)
        let rimGeometry = SCNTorus(ringRadius: topRadius - 0.02, pipeRadius: 0.05)
        let rimMaterial = SCNMaterial()
        rimMaterial.diffuse.contents = UIColor(red: 0.93, green: 0.81, blue: 0.58, alpha: 1.0)
        rimMaterial.lightingModel = .physicallyBased
        rimGeometry.materials = [rimMaterial]
        let rimNode = SCNNode(geometry: rimGeometry)
        rimNode.position = SCNVector3(0, height / 2.0, 0)
        rimNode.name = "coneRim"

        rootNode.addChildNode(coneNode)
        rootNode.addChildNode(rimNode)

        return rootNode
    }

    private static func createTruncatedConeGeometry(topRadius: CGFloat, bottomRadius: CGFloat, height: CGFloat, radialSegments: Int) -> SCNGeometry {
        var vertices: [SCNVector3] = []
        var normals: [SCNVector3] = []
        var uvs: [CGPoint] = []
        var indices: [Int32] = []

        let halfHeight = Float(height) / 2.0
        let rTop = Float(topRadius)
        let rBottom = Float(bottomRadius)

        // Generate side vertices
        for i in 0...radialSegments {
            let u = Float(i) / Float(radialSegments)
            let angle = u * 2.0 * Float.pi
            let cosA = cos(angle)
            let sinA = sin(angle)

            // Top ring vertex
            vertices.append(SCNVector3(rTop * cosA, halfHeight, rTop * sinA))
            normals.append(SCNVector3(cosA, 0.15, sinA))
            uvs.append(CGPoint(x: CGFloat(u * 6.0), y: 0.0))

            // Bottom ring vertex
            vertices.append(SCNVector3(rBottom * cosA, -halfHeight, rBottom * sinA))
            normals.append(SCNVector3(cosA, -0.15, sinA))
            uvs.append(CGPoint(x: CGFloat(u * 6.0), y: 4.0))
        }

        // Indices
        for i in 0..<radialSegments {
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
        let uvSource = SCNGeometrySource(textureCoordinates: uvs)
        let element = SCNGeometryElement(indices: indices, primitiveType: .triangles)

        return SCNGeometry(sources: [vertexSource, normalSource, uvSource], elements: [element])
    }

    // Generate procedural waffle diamond pattern
    private static func generateWaffleTexture() -> UIImage {
        let size = CGSize(width: 512, height: 512)
        let renderer = UIGraphicsImageRenderer(size: size)
        return renderer.image { ctx in
            let cgContext = ctx.cgContext
            
            // Base baked waffle color
            let baseColor = UIColor(red: 0.90, green: 0.77, blue: 0.54, alpha: 1.0)
            cgContext.setFillColor(baseColor.cgColor)
            cgContext.fill(CGRect(origin: .zero, size: size))

            // Draw diamond waffle grid lines
            let grooveColor = UIColor(red: 0.76, green: 0.60, blue: 0.38, alpha: 0.85)
            let highlightColor = UIColor(red: 0.97, green: 0.86, blue: 0.67, alpha: 0.6)

            let gridSpacing: CGFloat = 32
            cgContext.setLineWidth(4.0)

            // Diagonal lines positive
            for i in -16...32 {
                let startX = CGFloat(i) * gridSpacing
                cgContext.setStrokeColor(grooveColor.cgColor)
                cgContext.beginPath()
                cgContext.move(to: CGPoint(x: startX, y: 0))
                cgContext.addLine(to: CGPoint(x: startX + size.height, y: size.height))
                cgContext.strokePath()

                // Highlight border
                cgContext.setStrokeColor(highlightColor.cgColor)
                cgContext.beginPath()
                cgContext.move(to: CGPoint(x: startX + 2, y: 0))
                cgContext.addLine(to: CGPoint(x: startX + size.height + 2, y: size.height))
                cgContext.strokePath()
            }

            // Diagonal lines negative
            for i in 0...48 {
                let startX = CGFloat(i) * gridSpacing
                cgContext.setStrokeColor(grooveColor.cgColor)
                cgContext.beginPath()
                cgContext.move(to: CGPoint(x: startX, y: 0))
                cgContext.addLine(to: CGPoint(x: startX - size.height, y: size.height))
                cgContext.strokePath()

                cgContext.setStrokeColor(highlightColor.cgColor)
                cgContext.beginPath()
                cgContext.move(to: CGPoint(x: startX + 2, y: 0))
                cgContext.addLine(to: CGPoint(x: startX - size.height + 2, y: size.height))
                cgContext.strokePath()
            }
        }
    }
}
