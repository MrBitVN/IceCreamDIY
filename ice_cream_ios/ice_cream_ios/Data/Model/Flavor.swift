import SwiftUI
import UIKit

enum Flavor: String, CaseIterable, Identifiable {
    case vanilla = "Vanilla"
    case strawberry = "Strawberry"
    case chocolate = "Chocolate"
    case pistachio = "Pistachio"
    case blueberry = "Blueberry"

    var id: String { rawValue }

    var displayName: String {
        return rawValue
    }

    var color: Color {
        switch self {
        case .vanilla: return Color(red: 0.98, green: 0.98, blue: 0.96)
        case .strawberry: return Color(red: 0.98, green: 0.52, blue: 0.76)
        case .chocolate: return Color(red: 0.44, green: 0.28, blue: 0.18)
        case .pistachio: return Color(red: 0.65, green: 0.88, blue: 0.38)
        case .blueberry: return Color(red: 0.35, green: 0.78, blue: 0.95)
        }
    }

    var uiColor: UIColor {
        switch self {
        case .vanilla: return UIColor(red: 0.98, green: 0.98, blue: 0.96, alpha: 1.0)
        case .strawberry: return UIColor(red: 0.98, green: 0.52, blue: 0.76, alpha: 1.0)
        case .chocolate: return UIColor(red: 0.44, green: 0.28, blue: 0.18, alpha: 1.0)
        case .pistachio: return UIColor(red: 0.65, green: 0.88, blue: 0.38, alpha: 1.0)
        case .blueberry: return UIColor(red: 0.35, green: 0.78, blue: 0.95, alpha: 1.0)
        }
    }

    var slotIndex: Int {
        switch self {
        case .vanilla: return 0
        case .strawberry: return 1
        case .chocolate: return 2
        case .pistachio: return 3
        case .blueberry: return 4
        }
    }
}
