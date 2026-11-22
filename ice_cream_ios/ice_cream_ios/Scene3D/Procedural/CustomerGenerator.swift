import SceneKit
import UIKit

struct CustomerVariantColors {
    let name: String
    let skinColor: UIColor
    let hairColor: UIColor
    let shirtColor: UIColor
    let innerShirtColor: UIColor?
    let hasSunglasses: Bool
    let hasLongHair: Bool
}

final class CustomerGenerator {

    static let variants: [CustomerVariantColors] = [
        // Variant 0: Cool Girl with Purple/Blue Hair & Sunglasses (Image 7)
        CustomerVariantColors(
            name: "Cool Girl",
            skinColor: UIColor(red: 0.96, green: 0.82, blue: 0.72, alpha: 1.0),
            hairColor: UIColor(red: 0.35, green: 0.40, blue: 0.75, alpha: 1.0),
            shirtColor: UIColor(red: 0.22, green: 0.84, blue: 0.76, alpha: 1.0),
            innerShirtColor: nil,
            hasSunglasses: true,
            hasLongHair: true
        ),
        // Variant 1: Hipster Guy with Burgundy Jacket & Mint Shirt (Image 6)
        CustomerVariantColors(
            name: "Hipster Guy",
            skinColor: UIColor(red: 0.72, green: 0.52, blue: 0.40, alpha: 1.0),
            hairColor: UIColor(red: 0.24, green: 0.20, blue: 0.18, alpha: 1.0),
            shirtColor: UIColor(red: 0.65, green: 0.22, blue: 0.24, alpha: 1.0),
            innerShirtColor: UIColor(red: 0.24, green: 0.80, blue: 0.68, alpha: 1.0),
            hasSunglasses: false,
            hasLongHair: false
        ),
        // Variant 2: Sunny Blonde with Coral Top
        CustomerVariantColors(
            name: "Sunny Blonde",
            skinColor: UIColor(red: 0.96, green: 0.84, blue: 0.74, alpha: 1.0),
            hairColor: UIColor(red: 0.94, green: 0.82, blue: 0.38, alpha: 1.0),
            shirtColor: UIColor(red: 0.95, green: 0.45, blue: 0.52, alpha: 1.0),
            innerShirtColor: nil,
            hasSunglasses: false,
            hasLongHair: true
        ),
        // Variant 3: Trendy Guy with Yellow Polo & Shades
        CustomerVariantColors(
            name: "Trendy Guy",
            skinColor: UIColor(red: 0.82, green: 0.64, blue: 0.52, alpha: 1.0),
            hairColor: UIColor(red: 0.42, green: 0.28, blue: 0.18, alpha: 1.0),
            shirtColor: UIColor(red: 0.95, green: 0.72, blue: 0.18, alpha: 1.0),
            innerShirtColor: nil,
            hasSunglasses: true,
            hasLongHair: false
        ),
        // Variant 4: Sporty Teen with Orange Hoodie
        CustomerVariantColors(
            name: "Sporty Teen",
            skinColor: UIColor(red: 0.88, green: 0.72, blue: 0.58, alpha: 1.0),
            hairColor: UIColor(red: 0.18, green: 0.62, blue: 0.55, alpha: 1.0),
            shirtColor: UIColor(red: 0.96, green: 0.48, blue: 0.22, alpha: 1.0),
            innerShirtColor: UIColor(white: 0.98, alpha: 1.0),
            hasSunglasses: false,
            hasLongHair: false
        )
    ]

    static func createCustomerNode(variantIndex: Int = 0, isThumbsUp: Bool = false) -> SCNNode {
        let variant = variants[variantIndex % variants.count]
        let root = SCNNode()
        root.name = "customerRoot"

        // Torso / Chest
        let torsoGeo = SCNBox(width: 0.58, height: 0.65, length: 0.28, chamferRadius: 0.04)
        torsoGeo.firstMaterial?.diffuse.contents = variant.shirtColor
        let torsoNode = SCNNode(geometry: torsoGeo)
        torsoNode.position = SCNVector3(0, 0, 0)
        root.addChildNode(torsoNode)

        // Inner shirt (if dual layer, e.g. jacket)
        if let innerColor = variant.innerShirtColor {
            let innerGeo = SCNBox(width: 0.26, height: 0.55, length: 0.29, chamferRadius: 0.02)
            innerGeo.firstMaterial?.diffuse.contents = innerColor
            let innerNode = SCNNode(geometry: innerGeo)
            innerNode.position = SCNVector3(0, 0.02, 0.01)
            root.addChildNode(innerNode)
        }

        // Neck
        let neckGeo = SCNCylinder(radius: 0.08, height: 0.16)
        neckGeo.firstMaterial?.diffuse.contents = variant.skinColor
        let neckNode = SCNNode(geometry: neckGeo)
        neckNode.position = SCNVector3(0, 0.38, 0)
        root.addChildNode(neckNode)

        // Head
        let headGeo = SCNBox(width: 0.36, height: 0.40, length: 0.32, chamferRadius: 0.05)
        headGeo.firstMaterial?.diffuse.contents = variant.skinColor
        let headNode = SCNNode(geometry: headGeo)
        headNode.position = SCNVector3(0, 0.62, 0)
        root.addChildNode(headNode)

        // Hair
        let hairTopGeo = SCNBox(width: 0.39, height: 0.14, length: 0.35, chamferRadius: 0.03)
        hairTopGeo.firstMaterial?.diffuse.contents = variant.hairColor
        let hairTopNode = SCNNode(geometry: hairTopGeo)
        hairTopNode.position = SCNVector3(0, 0.78, 0)
        root.addChildNode(hairTopNode)

        if variant.hasLongHair {
            // Left long hair strand
            let hairLeftGeo = SCNBox(width: 0.08, height: 0.48, length: 0.24, chamferRadius: 0.02)
            hairLeftGeo.firstMaterial?.diffuse.contents = variant.hairColor
            let hairLeft = SCNNode(geometry: hairLeftGeo)
            hairLeft.position = SCNVector3(-0.21, 0.52, 0.02)
            root.addChildNode(hairLeft)

            // Right long hair strand
            let hairRight = hairLeft.clone()
            hairRight.position = SCNVector3(0.21, 0.52, 0.02)
            root.addChildNode(hairRight)
        }

        // Sunglasses (if applicable)
        if variant.hasSunglasses {
            let frameGeo = SCNBox(width: 0.34, height: 0.10, length: 0.04, chamferRadius: 0.01)
            frameGeo.firstMaterial?.diffuse.contents = UIColor(white: 0.12, alpha: 1.0)
            let frameNode = SCNNode(geometry: frameGeo)
            frameNode.position = SCNVector3(0, 0.64, 0.18)
            root.addChildNode(frameNode)
        }

        // Left Arm (holding cone in hand)
        let armRadius: CGFloat = 0.055
        let leftUpperArm = SCNCylinder(radius: armRadius, height: 0.38)
        leftUpperArm.firstMaterial?.diffuse.contents = variant.skinColor
        let leftUpperNode = SCNNode(geometry: leftUpperArm)
        leftUpperNode.position = SCNVector3(-0.35, -0.05, 0.15)
        leftUpperNode.eulerAngles = SCNVector3(-Float.pi / 4.0, 0, Float.pi / 12.0)
        root.addChildNode(leftUpperNode)

        // Right Arm (Thumbs up if 3 stars, else resting)
        let rightUpperArm = SCNCylinder(radius: armRadius, height: 0.38)
        rightUpperArm.firstMaterial?.diffuse.contents = variant.skinColor
        let rightUpperNode = SCNNode(geometry: rightUpperArm)

        if isThumbsUp {
            // Thumbs up pose: arm bent up towards front
            rightUpperNode.position = SCNVector3(0.36, 0.08, 0.18)
            rightUpperNode.eulerAngles = SCNVector3(Float.pi / 2.5, 0, -Float.pi / 10.0)

            // Thumb box
            let thumbGeo = SCNBox(width: 0.05, height: 0.10, length: 0.05, chamferRadius: 0.01)
            thumbGeo.firstMaterial?.diffuse.contents = variant.skinColor
            let thumbNode = SCNNode(geometry: thumbGeo)
            thumbNode.position = SCNVector3(0.38, 0.30, 0.22)
            thumbNode.eulerAngles = SCNVector3(0, 0, 0)
            root.addChildNode(thumbNode)
        } else {
            rightUpperNode.position = SCNVector3(0.35, -0.12, 0.05)
            rightUpperNode.eulerAngles = SCNVector3(-Float.pi / 10.0, 0, -Float.pi / 16.0)
        }
        root.addChildNode(rightUpperNode)

        return root
    }

    static func createDollarBillsNode(count: Int = 14) -> SCNNode {
        let parent = SCNNode()
        parent.name = "dollarBillsParent"

        let billWidth: CGFloat = 0.18
        let billHeight: CGFloat = 0.09
        let billColor = UIColor(red: 0.32, green: 0.72, blue: 0.42, alpha: 1.0)

        for _ in 0..<count {
            let billGeo = SCNPlane(width: billWidth, height: billHeight)
            billGeo.firstMaterial?.diffuse.contents = billColor
            billGeo.firstMaterial?.isDoubleSided = true

            let billNode = SCNNode(geometry: billGeo)
            let startX = Float.random(in: -0.45...0.45)
            let startY = Float.random(in: 1.2...2.2)
            let startZ = Float.random(in: -0.2...0.3)
            billNode.position = SCNVector3(startX, startY, startZ)
            billNode.eulerAngles = SCNVector3(
                Float.random(in: 0...Float.pi * 2),
                Float.random(in: 0...Float.pi * 2),
                Float.random(in: 0...Float.pi * 2)
            )

            // Falling physics action
            let fallDuration = Double.random(in: 1.0...1.6)
            let targetY = Float.random(in: -0.80 ... -0.68)
            let moveAction = SCNAction.move(to: SCNVector3(startX + Float.random(in: -0.1...0.1), targetY, startZ + Float.random(in: -0.1...0.1)), duration: fallDuration)
            moveAction.timingMode = .easeOut
            let rotateAction = SCNAction.rotateBy(
                x: CGFloat(Float.random(in: 1...4)),
                y: CGFloat(Float.random(in: 1...4)),
                z: CGFloat(Float.random(in: 1...3)),
                duration: fallDuration
            )
            billNode.runAction(SCNAction.group([moveAction, rotateAction]))

            parent.addChildNode(billNode)
        }

        return parent
    }
}
