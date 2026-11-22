import SwiftUI

struct ContentView: View {
    @StateObject private var viewModel = GameViewModel()

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
    }
}
