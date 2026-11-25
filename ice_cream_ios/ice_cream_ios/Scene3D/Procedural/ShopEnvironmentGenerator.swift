import SceneKit
import UIKit

final class ShopEnvironmentGenerator {

    static func createShopEnvironmentNode() -> SCNNode {
        let root = SCNNode()
        root.name = "shopEnvironmentRoot"

        // 1. Pink back wall
        let wallGeo = SCNPlane(width: 5.0, height: 6.0)
        wallGeo.firstMaterial?.diffuse.contents = UIColor(red: 1.0, green: 0.82, blue: 0.85, alpha: 1.0)
        let wallNode = SCNNode(geometry: wallGeo)
        wallNode.position = SCNVector3(0, 0.8, -1.8)
        root.addChildNode(wallNode)

        // Wall horizontal stripes (slats)
        for yOffset in stride(from: -0.6, through: 1.6, by: 0.45) {
            let stripeGeo = SCNBox(width: 5.0, height: 0.03, length: 0.02, chamferRadius: 0)
            stripeGeo.firstMaterial?.diffuse.contents = UIColor(red: 0.94, green: 0.72, blue: 0.76, alpha: 1.0)
            let stripeNode = SCNNode(geometry: stripeGeo)
            stripeNode.position = SCNVector3(0, Float(yOffset), -1.78)
            root.addChildNode(stripeNode)
        }

        // 2. Framed Ice cream poster on the back wall
        let posterFrameGeo = SCNBox(width: 0.65, height: 0.85, length: 0.04, chamferRadius: 0.02)
        posterFrameGeo.firstMaterial?.diffuse.contents = UIColor(white: 0.96, alpha: 1.0)
        let posterFrameNode = SCNNode(geometry: posterFrameGeo)
        posterFrameNode.position = SCNVector3(-0.05, 1.45, -1.76)
        root.addChildNode(posterFrameNode)

        let posterInsideGeo = SCNPlane(width: 0.55, height: 0.75)
        posterInsideGeo.firstMaterial?.diffuse.contents = UIColor(red: 0.98, green: 0.90, blue: 0.92, alpha: 1.0)
        let posterInside = SCNNode(geometry: posterInsideGeo)
        posterInside.position = SCNVector3(-0.05, 1.45, -1.73)
        root.addChildNode(posterInside)

        // Ice cream artwork inside the framed poster
        let artConeGeo = SCNCone(topRadius: 0.01, bottomRadius: 0.11, height: 0.24)
        artConeGeo.firstMaterial?.diffuse.contents = UIColor(red: 0.92, green: 0.71, blue: 0.44, alpha: 1.0)
        let artCone = SCNNode(geometry: artConeGeo)
        artCone.position = SCNVector3(-0.05, 1.32, -1.71)
        artCone.eulerAngles = SCNVector3(0, 0, Float.pi)
        root.addChildNode(artCone)

        let artScoopGeo = SCNSphere(radius: 0.12)
        artScoopGeo.firstMaterial?.diffuse.contents = UIColor(red: 1.0, green: 0.46, blue: 0.66, alpha: 1.0)
        let artScoop = SCNNode(geometry: artScoopGeo)
        artScoop.position = SCNVector3(-0.05, 1.50, -1.71)
        root.addChildNode(artScoop)

        // 3. Teal Soft-serve Machine on the left side (Image 5, 6)
        let machineBodyGeo = SCNBox(width: 0.72, height: 1.25, length: 0.6, chamferRadius: 0.04)
        machineBodyGeo.firstMaterial?.diffuse.contents = UIColor(red: 0.18, green: 0.75, blue: 0.78, alpha: 1.0)
        let machineNode = SCNNode(geometry: machineBodyGeo)
        machineNode.position = SCNVector3(-1.15, 0.45, -1.2)
        root.addChildNode(machineNode)

        // 3 dispenser levers
        for i in -1...1 {
            let leverGeo = SCNCylinder(radius: 0.02, height: 0.22)
            leverGeo.firstMaterial?.diffuse.contents = UIColor(white: 0.85, alpha: 1.0)
            let leverNode = SCNNode(geometry: leverGeo)
            leverNode.position = SCNVector3(-1.15 + (Float(i) * 0.12), 0.75, -0.88)
            leverNode.eulerAngles = SCNVector3(-Float.pi / 6.0, 0, 0)
            root.addChildNode(leverNode)
        }

        // 4. Wooden lollipop shelf on the upper right (Image 6, 7)
        let shelfGeo = SCNBox(width: 0.75, height: 0.05, length: 0.35, chamferRadius: 0.01)
        shelfGeo.firstMaterial?.diffuse.contents = UIColor(red: 0.75, green: 0.52, blue: 0.36, alpha: 1.0)
        let shelfNode = SCNNode(geometry: shelfGeo)
        shelfNode.position = SCNVector3(1.15, 1.35, -1.5)
        root.addChildNode(shelfNode)

        // Swirl lollipop
        let lollipopGeo = SCNCylinder(radius: 0.12, height: 0.03)
        lollipopGeo.firstMaterial?.diffuse.contents = UIColor(red: 0.95, green: 0.35, blue: 0.65, alpha: 1.0)
        let lollipopNode = SCNNode(geometry: lollipopGeo)
        lollipopNode.position = SCNVector3(1.15, 1.52, -1.48)
        lollipopNode.eulerAngles = SCNVector3(Float.pi / 2.0, 0, 0)
        root.addChildNode(lollipopNode)

        // 5. Pink counter table in front
        let counterGeo = SCNBox(width: 4.0, height: 0.8, length: 0.9, chamferRadius: 0.04)
        counterGeo.firstMaterial?.diffuse.contents = UIColor(red: 1.0, green: 0.42, blue: 0.62, alpha: 1.0)
        let counterNode = SCNNode(geometry: counterGeo)
        counterNode.position = SCNVector3(0, -1.15, -0.3)
        root.addChildNode(counterNode)

        return root
    }
}
