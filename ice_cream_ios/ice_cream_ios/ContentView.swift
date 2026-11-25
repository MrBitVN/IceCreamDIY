import SwiftUI

struct ContentView: View {
    @StateObject private var viewModel = GameViewModel()
    @Environment(\.scenePhase) private var scenePhase

    var body: some View {
        Group {
            switch viewModel.uiState.screen {
            case .splash:
                SplashScreen(viewModel: viewModel)
            case .home:
                HomeScreen(viewModel: viewModel)
            case .game:
                GameScreen(viewModel: viewModel)
            }
        }
        .onChange(of: scenePhase) { newPhase in
            if newPhase == .background || newPhase == .inactive {
                viewModel.onAppBackgrounded()
            }
        }
    }
}
