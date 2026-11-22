import SwiftUI

struct SettingsDialogView: View {
    let level: Int
    let coins: Int
    let isSoundEnabled: Bool
    let isHapticsEnabled: Bool
    let onToggleSound: () -> Void
    let onToggleHaptics: () -> Void
    let onResetProgress: () -> Void
    let onDismiss: () -> Void

    var body: some View {
        ZStack {
            Color.black.opacity(0.4)
                .ignoresSafeArea()
                .onTapGesture { onDismiss() }

            VStack(spacing: 20) {
                Text("SETTINGS")
                    .font(.system(size: 24, weight: .black, design: .rounded))
                    .foregroundColor(Color(red: 0.28, green: 0.28, blue: 0.28))

                VStack(spacing: 14) {
                    // Sound Toggle
                    HStack {
                        Text("Sound Effects")
                            .font(.system(size: 16, weight: .bold, design: .rounded))
                            .foregroundColor(Color(red: 0.35, green: 0.35, blue: 0.35))
                        Spacer()
                        Toggle("", isOn: Binding(
                            get: { isSoundEnabled },
                            set: { _ in onToggleSound() }
                        ))
                        .labelsHidden()
                        .tint(Color(red: 1.0, green: 0.32, blue: 0.47))
                    }

                    // Haptics Toggle
                    HStack {
                        Text("Vibration & Haptics")
                            .font(.system(size: 16, weight: .bold, design: .rounded))
                            .foregroundColor(Color(red: 0.35, green: 0.35, blue: 0.35))
                        Spacer()
                        Toggle("", isOn: Binding(
                            get: { isHapticsEnabled },
                            set: { _ in onToggleHaptics() }
                        ))
                        .labelsHidden()
                        .tint(Color(red: 1.0, green: 0.32, blue: 0.47))
                    }
                }
                .padding(.horizontal, 4)

                // Stats Pill
                HStack {
                    Spacer()
                    Text("Level: \(level)")
                        .font(.system(size: 15, weight: .bold, design: .rounded))
                        .foregroundColor(Color(red: 1.0, green: 0.32, blue: 0.47))
                    Spacer()
                    Text("Coins: \(coins)")
                        .font(.system(size: 15, weight: .bold, design: .rounded))
                        .foregroundColor(Color(red: 1.0, green: 0.62, blue: 0.0))
                    Spacer()
                }
                .padding(.vertical, 12)
                .background(Color(red: 1.0, green: 0.94, blue: 0.95))
                .cornerRadius(14)

                // Reset Progress
                Button(action: onResetProgress) {
                    Text("Reset Progress")
                        .font(.system(size: 15, weight: .bold, design: .rounded))
                        .foregroundColor(Color.red)
                        .frame(maxWidth: .infinity)
                        .frame(height: 44)
                        .background(Color(red: 1.0, green: 0.90, blue: 0.92))
                        .cornerRadius(22)
                }

                // Close Button
                Button(action: onDismiss) {
                    Text("OK")
                        .font(.system(size: 18, weight: .black, design: .rounded))
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .frame(height: 48)
                        .background(Color(red: 1.0, green: 0.32, blue: 0.47))
                        .cornerRadius(24)
                        .shadow(color: Color(red: 1.0, green: 0.32, blue: 0.47).opacity(0.4), radius: 6, y: 3)
                }
            }
            .padding(24)
            .background(Color.white)
            .cornerRadius(28)
            .overlay(
                RoundedRectangle(cornerRadius: 28)
                    .stroke(Color(red: 1.0, green: 0.55, blue: 0.63), lineWidth: 3)
            )
            .shadow(color: Color.black.opacity(0.18), radius: 16)
            .padding(.horizontal, 28)
        }
    }
}
