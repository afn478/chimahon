import ChimahonShared
import SwiftUI
import UIKit

@main
struct ChimahonIOSApp: App {
    init() {
        _ = ChimahonIosBackgroundTasks.shared.register()
    }

    var body: some Scene {
        WindowGroup {
            ComposeView()
                .ignoresSafeArea(.keyboard)
        }
    }
}

private struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
    }
}
