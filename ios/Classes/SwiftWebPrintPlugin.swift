import Flutter
import UIKit

public class SwiftWebPrintPlugin: NSObject, FlutterPlugin {
  public static func register(with registrar: FlutterPluginRegistrar) {
    let channel = FlutterMethodChannel(name: "web_print", binaryMessenger: registrar.messenger())
    let instance = SwiftWebPrintPlugin()
    registrar.addMethodCallDelegate(instance, channel: channel)
  }

  public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
    if call.method == "printWebUrl" {
        do {
            try printText(text: "Nurullah")
            result("Başarılı")
        } catch let error {
            result(error)
        }
        
    }
  }
    
    private func printText(text:String) throws{
//         1
        let printController = UIPrintInteractionController.shared
        // 2
        let printInfo = UIPrintInfo(dictionary:nil)
        printInfo.outputType = .general
        printInfo.jobName = "print Job"
        printController.printInfo = printInfo
        
        let url = URL(string: "https://akuple.com/fatura")
        let contents = try String(contentsOf: url!,encoding: .utf8)
        
        // 3
        let formatter = UIMarkupTextPrintFormatter(markupText: contents)
        formatter.perPageContentInsets = UIEdgeInsets(top: 72, left: 72, bottom: 72, right: 72)
        printController.printFormatter = formatter

        // 4
        printController.present(animated: true, completionHandler: nil)
       
       
    }
}
