import Foundation
import SceneKit
import UIKit

final class MeshExtruder {
    let ringSegments: Int
    let maxRings: Int
    let lobes: Int

    private(set) var currentRingCount: Int = 0

    // Preallocated buffers
    private var positions: [SCNVector3]
    private var normals: [SCNVector3]
    private var colors: [UInt32] // RGBA8888 or float vector
    private(set) var indices: [Int32]

    init(ringSegments: Int = 16, maxRings: Int = 180, lobes: Int = 6) {
        self.ringSegments = ringSegments
        self.maxRings = maxRings
        self.lobes = lobes

        let totalVertices = maxRings * ringSegments
        let totalIndices = (maxRings - 1) * ringSegments * 6

        self.positions = Array(repeating: SCNVector3Zero, count: totalVertices)
        self.normals = Array(repeating: SCNVector3(0, 1, 0), count: totalVertices)
        self.colors = Array(repeating: 0xFFFFFFFF, count: totalVertices)
        self.indices = Array(repeating: 0, count: totalIndices)

        // Prebuild index array
        var idx = 0
        for ring in 0..<(maxRings - 1) {
            let ringStart = ring * ringSegments
            let nextRingStart = (ring + 1) * ringSegments

            for seg in 0..<ringSegments {
                let nextSeg = (seg + 1) % ringSegments

                let p1 = Int32(ringStart + seg)
                let p2 = Int32(ringStart + nextSeg)
                let p3 = Int32(nextRingStart + seg)
                let p4 = Int32(nextRingStart + nextSeg)

                indices[idx] = p1; idx += 1
                indices[idx] = p3; idx += 1
                indices[idx] = p2; idx += 1

                indices[idx] = p2; idx += 1
                indices[idx] = p3; idx += 1
                indices[idx] = p4; idx += 1
            }
        }
    }

    func reset() {
        currentRingCount = 0
    }

    @discardableResult
    func appendRing(t: Float, activeFlavors: [Flavor]) -> Int {
        guard currentRingCount < maxRings else { return -1 }

        let ringIndex = currentRingCount
        let totalTurns: Float = 4.2
        let angle = t * totalTurns * 2.0 * Float.pi

        // Helix centerline tapers from 0.38 down to 0.08
        let helixRadius = 0.38 * (1.0 - 0.76 * t)
        let cx = helixRadius * cos(angle)
        let cz = helixRadius * sin(angle)
        let cy = t * 1.55

        // Tangent approximation
        let nextT = t + 0.01
        let nextAngle = nextT * totalTurns * 2.0 * Float.pi
        let nextRadius = 0.38 * (1.0 - 0.76 * nextT)
        let ncx = nextRadius * cos(nextAngle)
        let ncz = nextRadius * sin(nextAngle)
        let ncy = nextT * 1.55

        let tx = ncx - cx
        let ty = ncy - cy
        let tz = ncz - cz
        let tLen = max(sqrt(tx * tx + ty * ty + tz * tz), 0.0001)
        let normTx = tx / tLen
        let normTy = ty / tLen
        let normTz = tz / tLen

        // Up vector
        let upX: Float = 0.0
        let upY: Float = 1.0
        let upZ: Float = 0.0

        // Normal = Tangent x Up
        var nx = normTy * upZ - normTz * upY
        var ny = normTz * upX - normTx * upZ
        var nz = normTx * upY - normTy * upX
        let nLen = max(sqrt(nx * nx + ny * ny + nz * nz), 0.0001)
        nx /= nLen
        ny /= nLen
        nz /= nLen

        // Binormal = Normal x Tangent
        let bx = ny * normTz - nz * normTy
        let by = nz * normTx - nx * normTz
        let bz = nx * normTy - ny * normTx

        let tubeRadius = 0.16 * (1.0 - 0.45 * t)
        let flavorsToUse = activeFlavors.isEmpty ? [Flavor.vanilla] : activeFlavors
        let numFlavors = flavorsToUse.count

        let baseVertexOffset = ringIndex * ringSegments

        for seg in 0..<ringSegments {
            let phi = (Float(seg) / Float(ringSegments)) * 2.0 * Float.pi

            // Star nozzle fluting
            let flute = 1.0 + 0.15 * cos(Float(lobes) * phi)
            let rCurrent = tubeRadius * flute

            let cosPhi = cos(phi)
            let sinPhi = sin(phi)

            let rx = cosPhi * nx + sinPhi * bx
            let ry = cosPhi * ny + sinPhi * by
            let rz = cosPhi * nz + sinPhi * bz

            let px = cx + rCurrent * rx
            let py = cy + rCurrent * ry
            let pz = cz + rCurrent * rz

            let normLen = max(sqrt(rx * rx + ry * ry + rz * rz), 0.0001)
            let vnx = rx / normLen
            let vny = ry / normLen
            let vnz = rz / normLen

            let flavorIndex = Int((phi / (2.0 * Float.pi)) * Float(numFlavors)).clamped(to: 0...(numFlavors - 1))
            let flavor = flavorsToUse[flavorIndex]

            let vIdx = baseVertexOffset + seg
            positions[vIdx] = SCNVector3(px, py, pz)
            normals[vIdx] = SCNVector3(vnx, vny, vnz)
            
            // Store flavor color
            let uiCol = flavor.uiColor
            var r: CGFloat = 0; var g: CGFloat = 0; var b: CGFloat = 0; var a: CGFloat = 0
            uiCol.getRed(&r, green: &g, blue: &b, alpha: &a)
            let rByte = UInt32(r * 255.0)
            let gByte = UInt32(g * 255.0)
            let bByte = UInt32(b * 255.0)
            let aByte = UInt32(a * 255.0)
            colors[vIdx] = (rByte) | (gByte << 8) | (bByte << 16) | (aByte << 24)
        }

        currentRingCount += 1
        return ringIndex
    }

    func buildGeometry() -> SCNGeometry? {
        guard currentRingCount >= 2 else { return nil }

        let validVertexCount = currentRingCount * ringSegments
        let validIndexCount = (currentRingCount - 1) * ringSegments * 6

        let posData = Data(bytes: positions, count: validVertexCount * MemoryLayout<SCNVector3>.size)
        let normData = Data(bytes: normals, count: validVertexCount * MemoryLayout<SCNVector3>.size)
        let colorData = Data(bytes: colors, count: validVertexCount * MemoryLayout<UInt32>.size)
        let indexData = Data(bytes: indices, count: validIndexCount * MemoryLayout<Int32>.size)

        let posSource = SCNGeometrySource(
            data: posData,
            semantic: .vertex,
            vectorCount: validVertexCount,
            usesFloatComponents: true,
            componentsPerVector: 3,
            bytesPerComponent: MemoryLayout<Float>.size,
            dataOffset: 0,
            dataStride: MemoryLayout<SCNVector3>.size
        )

        let normSource = SCNGeometrySource(
            data: normData,
            semantic: .normal,
            vectorCount: validVertexCount,
            usesFloatComponents: true,
            componentsPerVector: 3,
            bytesPerComponent: MemoryLayout<Float>.size,
            dataOffset: 0,
            dataStride: MemoryLayout<SCNVector3>.size
        )

        let colorSource = SCNGeometrySource(
            data: colorData,
            semantic: .color,
            vectorCount: validVertexCount,
            usesFloatComponents: false,
            componentsPerVector: 4,
            bytesPerComponent: 1,
            dataOffset: 0,
            dataStride: MemoryLayout<UInt32>.size
        )

        let element = SCNGeometryElement(
            data: indexData,
            primitiveType: .triangles,
            primitiveCount: validIndexCount / 3,
            bytesPerIndex: MemoryLayout<Int32>.size
        )

        let geometry = SCNGeometry(sources: [posSource, normSource, colorSource], elements: [element])
        let mat = SCNMaterial()
        mat.diffuse.contents = UIColor.white
        mat.roughness.contents = 0.35
        mat.lightingModel = .physicallyBased
        geometry.materials = [mat]

        return geometry
    }
}

private extension Comparable {
    func clamped(to limits: ClosedRange<Self>) -> Self {
        return min(max(self, limits.lowerBound), limits.upperBound)
    }
}
