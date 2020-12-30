import 'dart:async';

import 'package:flutter/services.dart';

class WebPrint {
  static const MethodChannel _channel = const MethodChannel('web_print');

  static Future<String> get platformVersion async {
    final String version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }

  static Future printWebUrl(String url) async {
    await _channel.invokeMethod('printWebUrl', {'url': url});
  }
}
