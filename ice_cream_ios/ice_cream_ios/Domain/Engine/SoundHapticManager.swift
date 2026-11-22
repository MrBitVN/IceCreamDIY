import Foundation
import UIKit
import AVFoundation

final class SoundHapticManager {
    private let impactFeedback = UIImpactFeedbackGenerator(style: .light)
    private let notificationFeedback = UINotificationFeedbackGenerator()
    private var lastHapticTime: TimeInterval = 0
    private var isPlaying = false

    var isSoundEnabled: Bool = true {
        didSet {
            if !isSoundEnabled && isPlaying {
                stopPouringFeedback()
            }
        }
    }
    var isHapticsEnabled: Bool = true

    private var audioEngine: AVAudioEngine?
    private var playerNode: AVAudioPlayerNode?

    init() {
        impactFeedback.prepare()
        notificationFeedback.prepare()
        setupAudio()
    }

    private func setupAudio() {
        let engine = AVAudioEngine()
        let player = AVAudioPlayerNode()
        engine.attach(player)

        let sampleRate: Double = 44100
        guard let format = AVAudioFormat(standardFormatWithSampleRate: sampleRate, channels: 1) else { return }

        let frameCount = AVAudioFrameCount(sampleRate)
        guard let buffer = AVAudioPCMBuffer(pcmFormat: format, frameCapacity: frameCount) else { return }
        buffer.frameLength = frameCount

        let channels = buffer.floatChannelData?[0]
        for i in 0..<Int(frameCount) {
            let t = Double(i) / sampleRate
            let val = sin(2.0 * Double.pi * 160.0 * t) * 0.25 + sin(2.0 * Double.pi * 320.0 * t) * 0.1
            channels?[i] = Float(val)
        }

        engine.connect(player, to: engine.mainMixerNode, format: format)
        do {
            try engine.start()
            player.scheduleBuffer(buffer, at: nil, options: .loops, completionHandler: nil)
            self.audioEngine = engine
            self.playerNode = player
        } catch {
        }
    }

    func startPouringFeedback() {
        if isSoundEnabled && !isPlaying {
            playerNode?.play()
            isPlaying = true
        }

        if isHapticsEnabled {
            let now = Date().timeIntervalSince1970
            if now - lastHapticTime > 0.08 {
                lastHapticTime = now
                impactFeedback.impactOccurred(intensity: 0.5)
            }
        }
    }

    func stopPouringFeedback() {
        if isPlaying {
            playerNode?.pause()
            isPlaying = false
        }
    }

    func playSuccessFeedback() {
        if isHapticsEnabled {
            notificationFeedback.notificationOccurred(.success)
        }
    }

    func release() {
        stopPouringFeedback()
        audioEngine?.stop()
        audioEngine = nil
        playerNode = nil
    }
}
