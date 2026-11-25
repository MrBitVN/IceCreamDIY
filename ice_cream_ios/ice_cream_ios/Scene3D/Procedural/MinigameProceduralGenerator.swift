import SceneKit
import UIKit

final class MinigameProceduralGenerator {

    static func createPinkCuttingBoardNode() -> SCNNode {
        let root = SCNNode()
        root.name = "pinkCuttingBoard"

        // Rounded pink wooden board using UIBezierPath extruded with SCNShape
        let width: CGFloat = 2.62
        let depth: CGFloat = 1.28
        let cornerR: CGFloat = 0.24
        let rect = CGRect(x: -width / 2, y: -depth / 2, width: width, height: depth)
        let path = UIBezierPath(roundedRect: rect, cornerRadius: cornerR)

        let shape = SCNShape(path: path, extrusionDepth: 0.12)
        shape.chamferRadius = 0.025
        let boardMat = SCNMaterial()
        boardMat.diffuse.contents = UIColor(red: 0.98, green: 0.73, blue: 0.81, alpha: 1.0)
        boardMat.roughness.contents = 0.65
        boardMat.lightingModel = .physicallyBased
        shape.materials = [boardMat]

        let boardNode = SCNNode(geometry: shape)
        boardNode.eulerAngles = SCNVector3(-Float.pi / 2.0, 0, 0)
        root.addChildNode(boardNode)

        // Handle cutout slot on left side
        let holeGeo = SCNBox(width: 0.11, height: 0.025, length: 0.42, chamferRadius: 0.04)
        let holeMat = SCNMaterial()
        holeMat.diffuse.contents = UIColor(red: 0.92, green: 0.38, blue: 0.55, alpha: 1.0)
        holeMat.lightingModel = .physicallyBased
        holeGeo.materials = [holeMat]

        let holeNode = SCNNode(geometry: holeGeo)
        holeNode.position = SCNVector3(-1.08, 0.055, 0)
        root.addChildNode(holeNode)

        return root
    }

    static func createSortingBoxNode(compartmentFloorNodes: inout [SCNNode]) -> SCNNode {
        let root = SCNNode()
        root.name = "sortingBox"

        let boxW: CGFloat = 2.52
        let boxD: CGFloat = 1.36
        let wallH: CGFloat = 0.28
        let wallThick: CGFloat = 0.06

        let woodMat = SCNMaterial()
        woodMat.diffuse.contents = UIColor(red: 0.72, green: 0.47, blue: 0.27, alpha: 1.0)
        woodMat.roughness.contents = 0.70
        woodMat.lightingModel = .physicallyBased

        compartmentFloorNodes.removeAll()
        for ing in MinigameIngredient.allCases {
            let floorGeo = SCNBox(width: 0.80, height: 0.05, length: boxD - 0.08, chamferRadius: 0.01)
            let floorMat = SCNMaterial()
            floorMat.diffuse.contents = UIColor(red: 0.94, green: 0.81, blue: 0.64, alpha: 1.0)
            floorMat.roughness.contents = 0.75
            floorMat.lightingModel = .physicallyBased
            floorGeo.materials = [floorMat]

            let fNode = SCNNode(geometry: floorGeo)
            fNode.position = SCNVector3(ing.targetCenterX, 0.03, 0)
            root.addChildNode(fNode)
            compartmentFloorNodes.append(fNode)
        }

        // Front & Back walls
        let fbGeo = SCNBox(width: boxW, height: wallH, length: wallThick, chamferRadius: 0.01)
        fbGeo.materials = [woodMat]

        let frontWall = SCNNode(geometry: fbGeo)
        frontWall.position = SCNVector3(0, Float(wallH / 2), Float(boxD / 2))
        root.addChildNode(frontWall)

        let backWall = SCNNode(geometry: fbGeo)
        backWall.position = SCNVector3(0, Float(wallH / 2), Float(-boxD / 2))
        root.addChildNode(backWall)

        // Left, Right & 2 Inner Divider walls
        let lrGeo = SCNBox(width: wallThick, height: wallH, length: boxD, chamferRadius: 0.01)
        lrGeo.materials = [woodMat]
        let wallXs: [Float] = [Float(-boxW / 2), -0.41, 0.41, Float(boxW / 2)]
        for wx in wallXs {
            let wNode = SCNNode(geometry: lrGeo)
            wNode.position = SCNVector3(wx, Float(wallH / 2), 0)
            root.addChildNode(wNode)
        }

        // Miniature sample hints inside each compartment
        let mintSample = createSlicedIngredientNode(for: .mint)
        mintSample.scale = SCNVector3(0.55, 0.55, 0.55)
        mintSample.position = SCNVector3(-0.96, 0.08, -0.38)
        root.addChildNode(mintSample)

        let mangoSample = createSlicedIngredientNode(for: .mango)
        mangoSample.scale = SCNVector3(0.55, 0.55, 0.55)
        mangoSample.position = SCNVector3(-0.16, 0.08, -0.38)
        root.addChildNode(mangoSample)

        let berrySample = createSlicedIngredientNode(for: .blueberry)
        berrySample.scale = SCNVector3(0.55, 0.55, 0.55)
        berrySample.position = SCNVector3(0.66, 0.08, -0.38)
        root.addChildNode(berrySample)

        return root
    }

    static func createWholeIngredientNode(for ingredient: MinigameIngredient) -> SCNNode {
        let root = SCNNode()
        switch ingredient {
        case .blueberry:
            let offsets: [(Float, Float, Float)] = [(-0.09, 0.01, -0.06), (0.09, 0.01, -0.04), (0.0, 0.05, 0.08)]
            for (idx, off) in offsets.enumerated() {
                let sphere = SCNSphere(radius: 0.11)
                sphere.segmentCount = 14
                let mat = SCNMaterial()
                mat.diffuse.contents = idx == 2
                    ? UIColor(red: 0.26, green: 0.36, blue: 0.78, alpha: 1.0)
                    : UIColor(red: 0.16, green: 0.22, blue: 0.54, alpha: 1.0)
                mat.roughness.contents = 0.35
                mat.lightingModel = .physicallyBased
                sphere.materials = [mat]

                let bNode = SCNNode(geometry: sphere)
                bNode.scale = SCNVector3(1.0, 0.88, 1.0)
                bNode.position = SCNVector3(off.0, off.1, off.2)
                root.addChildNode(bNode)
            }

        case .mint:
            let angles: [Float] = [-0.65, 0.65, 0.0]
            let leafGeo = createProceduralMintLeafGeometry()
            for (idx, ang) in angles.enumerated() {
                let lNode = SCNNode(geometry: leafGeo)
                lNode.eulerAngles = SCNVector3(0, ang, 0)
                lNode.position = SCNVector3(0, Float(idx) * 0.015, 0)
                root.addChildNode(lNode)
            }

        case .mango:
            let sphere = SCNSphere(radius: 0.17)
            sphere.segmentCount = 16
            let mat = SCNMaterial()
            mat.diffuse.contents = UIColor(red: 1.0, green: 0.70, blue: 0.10, alpha: 1.0)
            mat.roughness.contents = 0.40
            mat.lightingModel = .physicallyBased
            sphere.materials = [mat]

            let mNode = SCNNode(geometry: sphere)
            mNode.scale = SCNVector3(1.25, 0.88, 0.90)
            mNode.eulerAngles = SCNVector3(0, -0.35, 0)
            root.addChildNode(mNode)
        }
        return root
    }

    static func createSlicedIngredientNode(for ingredient: MinigameIngredient) -> SCNNode {
        let root = SCNNode()
        let offsets: [(Float, Float, Float)] = [(-0.13, 0.01, -0.08), (0.13, 0.01, -0.06), (0.0, 0.01, 0.12)]
        let rots: [Float] = [-0.38, 0.48, 0.14]

        for i in 0..<3 {
            let child: SCNNode
            switch ingredient {
            case .blueberry:
                let s = SCNSphere(radius: 0.095)
                s.segmentCount = 12
                let mat = SCNMaterial()
                mat.diffuse.contents = UIColor(red: 0.26, green: 0.36, blue: 0.78, alpha: 1.0)
                mat.roughness.contents = 0.35
                mat.lightingModel = .physicallyBased
                s.materials = [mat]
                child = SCNNode(geometry: s)
                child.scale = SCNVector3(1.0, 0.68, 1.0)

            case .mint:
                let leafGeo = createProceduralMintLeafGeometry()
                child = SCNNode(geometry: leafGeo)
                child.scale = SCNVector3(0.75, 0.80, 0.75)

            case .mango:
                let cube = SCNBox(width: 0.14, height: 0.12, length: 0.14, chamferRadius: 0.025)
                let mat = SCNMaterial()
                mat.diffuse.contents = UIColor(red: 1.0, green: 0.70, blue: 0.10, alpha: 1.0)
                mat.roughness.contents = 0.35
                mat.lightingModel = .physicallyBased
                cube.materials = [mat]
                child = SCNNode(geometry: cube)
            }
            child.position = SCNVector3(offsets[i].0, offsets[i].1, offsets[i].2)
            child.eulerAngles = SCNVector3(0, rots[i], 0)
            root.addChildNode(child)
        }
        return root
    }

    static func createChefKnifeNode() -> SCNNode {
        let root = SCNNode()
        root.name = "chefKnife"

        // Wedge metallic blade
        let bladeGeo = SCNBox(width: 0.024, height: 0.16, length: 0.68, chamferRadius: 0.006)
        let silverMat = SCNMaterial()
        silverMat.diffuse.contents = UIColor(red: 0.90, green: 0.93, blue: 0.96, alpha: 1.0)
        silverMat.metalness.contents = 0.88
        silverMat.roughness.contents = 0.22
        silverMat.lightingModel = .physicallyBased
        bladeGeo.materials = [silverMat]

        let bladeNode = SCNNode(geometry: bladeGeo)
        bladeNode.position = SCNVector3(0, 0, -0.22)
        root.addChildNode(bladeNode)

        // Pastel pink handle
        let handleGeo = SCNCylinder(radius: 0.048, height: 0.28)
        let handleMat = SCNMaterial()
        handleMat.diffuse.contents = UIColor(red: 1.0, green: 0.46, blue: 0.66, alpha: 1.0)
        handleMat.roughness.contents = 0.45
        handleMat.lightingModel = .physicallyBased
        handleGeo.materials = [handleMat]

        let handleNode = SCNNode(geometry: handleGeo)
        handleNode.eulerAngles = SCNVector3(Float.pi / 2.0, 0, 0)
        handleNode.position = SCNVector3(0, 0.03, 0.22)
        root.addChildNode(handleNode)

        return root
    }

    static func createBottomAnchoredTaperedGeometry(
        bottomRadius: Float,
        topRadius: Float,
        height: Float,
        segments: Int = 24,
        color: UIColor,
        alpha: CGFloat = 1.0
    ) -> SCNGeometry {
        var vertices: [SCNVector3] = []
        var normals: [SCNVector3] = []
        var indices: [Int32] = []

        for i in 0...segments {
            let u = Float(i) / Float(segments)
            let angle = u * 2.0 * Float.pi
            let ca = cos(angle)
            let sa = sin(angle)

            vertices.append(SCNVector3(topRadius * ca, height, topRadius * sa))
            normals.append(SCNVector3(ca, 0.12, sa))

            vertices.append(SCNVector3(bottomRadius * ca, 0, bottomRadius * sa))
            normals.append(SCNVector3(ca, -0.12, sa))
        }

        for i in 0..<segments {
            let p1 = Int32(i * 2)
            let p2 = Int32(i * 2 + 1)
            let p3 = Int32((i + 1) * 2)
            let p4 = Int32((i + 1) * 2 + 1)
            indices.append(contentsOf: [p1, p2, p3, p3, p2, p4])
        }

        // Top cap if opaque liquid
        if alpha > 0.95 {
            let centerIdx = Int32(vertices.count)
            vertices.append(SCNVector3(0, height, 0))
            normals.append(SCNVector3(0, 1, 0))
            for i in 0...segments {
                let angle = (Float(i) / Float(segments)) * 2.0 * Float.pi
                vertices.append(SCNVector3(topRadius * cos(angle), height, topRadius * sin(angle)))
                normals.append(SCNVector3(0, 1, 0))
            }
            for i in 1...segments {
                indices.append(contentsOf: [centerIdx, centerIdx + Int32(i + 1), centerIdx + Int32(i)])
            }
        }

        let vSrc = SCNGeometrySource(vertices: vertices)
        let nSrc = SCNGeometrySource(normals: normals)
        let elem = SCNGeometryElement(indices: indices, primitiveType: .triangles)
        let geo = SCNGeometry(sources: [vSrc, nSrc], elements: [elem])

        let mat = SCNMaterial()
        mat.diffuse.contents = color.withAlphaComponent(alpha)
        mat.roughness.contents = alpha < 0.95 ? 0.15 : 0.35
        mat.lightingModel = .physicallyBased
        if alpha < 0.95 {
            mat.isDoubleSided = true
            mat.writesToDepthBuffer = false
        }
        geo.materials = [mat]
        return geo
    }

    private static func createProceduralMintLeafGeometry() -> SCNGeometry {
        let length: Float = 0.32
        let width: Float = 0.21
        let arch: Float = 0.055
        let hl = length * 0.5
        let hw = width * 0.5

        let verts: [SCNVector3] = [
            SCNVector3(0, 0.01, -hl),
            SCNVector3(-hw * 0.75, arch * 0.6, -hl * 0.35),
            SCNVector3(0, arch, -hl * 0.25),
            SCNVector3(hw * 0.75, arch * 0.6, -hl * 0.35),
            SCNVector3(-hw, arch * 0.7, hl * 0.20),
            SCNVector3(0, arch * 1.1, hl * 0.20),
            SCNVector3(hw, arch * 0.7, hl * 0.20),
            SCNVector3(0, 0.02, hl)
        ]
        let norms = Array(repeating: SCNVector3(0, 1, 0), count: verts.count)
        let indices: [Int32] = [
            0, 2, 1,
            0, 3, 2,
            1, 2, 4,
            2, 5, 4,
            2, 3, 6,
            2, 6, 5,
            4, 5, 7,
            5, 6, 7
        ]

        let geo = SCNGeometry(
            sources: [SCNGeometrySource(vertices: verts), SCNGeometrySource(normals: norms)],
            elements: [SCNGeometryElement(indices: indices, primitiveType: .triangles)]
        )
        let mat = SCNMaterial()
        mat.diffuse.contents = UIColor(red: 0.20, green: 0.80, blue: 0.42, alpha: 1.0)
        mat.roughness.contents = 0.45
        mat.isDoubleSided = true
        mat.lightingModel = .physicallyBased
        geo.materials = [mat]
        return geo
    }
}
