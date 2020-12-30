#import "WebPrintPlugin.h"
#if __has_include(<web_print/web_print-Swift.h>)
#import <web_print/web_print-Swift.h>
#else
// Support project import fallback if the generated compatibility header
// is not copied when this plugin is created as a library.
// https://forums.swift.org/t/swift-static-libraries-dont-copy-generated-objective-c-header/19816
#import "web_print-Swift.h"
#endif

@implementation WebPrintPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftWebPrintPlugin registerWithRegistrar:registrar];
}
@end
