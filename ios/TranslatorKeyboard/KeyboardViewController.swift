import UIKit
import Flutter

class KeyboardViewController: UIInputViewController {

    private var flutterEngine: FlutterEngine!
    private var flutterVC: FlutterViewController!
    private var channel: FlutterMethodChannel!

    override func viewDidLoad() {
        super.viewDidLoad()
        setupFlutterEngine()
        setupMethodChannel()
        embedFlutterView()
    }

    private func setupFlutterEngine() {
        flutterEngine = FlutterEngine(name: "keyboard_engine", project: nil)
        // Use named entrypoint matching keyboard_main.dart
        flutterEngine.run(withEntrypoint: "keyboardMain", libraryURI: nil)
        flutterVC = FlutterViewController(engine: flutterEngine, nibName: nil, bundle: nil)
    }

    private func setupMethodChannel() {
        channel = FlutterMethodChannel(
            name: "translator_keyboard/actions",
            binaryMessenger: flutterEngine.binaryMessenger
        )
        channel.setMethodCallHandler { [weak self] call, result in
            guard let self else { return }
            switch call.method {
            case "injectText":
                if let args = call.arguments as? [String: Any],
                   let text = args["text"] as? String {
                    self.textDocumentProxy.insertText(text)
                }
                result(nil)
            case "dismissKeyboard":
                self.dismissKeyboard()
                result(nil)
            case "deleteLastChar":
                self.textDocumentProxy.deleteBackward()
                result(nil)
            default:
                result(FlutterMethodNotImplemented)
            }
        }
    }

    private func embedFlutterView() {
        addChild(flutterVC)
        view.addSubview(flutterVC.view)
        flutterVC.view.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            flutterVC.view.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            flutterVC.view.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            flutterVC.view.topAnchor.constraint(equalTo: view.topAnchor),
            flutterVC.view.bottomAnchor.constraint(equalTo: view.bottomAnchor),
        ])
        flutterVC.didMove(toParent: self)

        // Set preferred height (keyboard panel height)
        let heightConstraint = view.heightAnchor.constraint(equalToConstant: 260)
        heightConstraint.priority = .required
        heightConstraint.isActive = true
    }
}
