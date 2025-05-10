import 'dart:async';

import 'package:flutter/services.dart';
import 'package:web_print/models/PrinterBluetoothDevice.dart';

class WebPrint {
  static const MethodChannel _channel = const MethodChannel('web_print');

  static Future printWebView(
      {String? url,
      String? html,
      required String printerAddress,
      int? topOffset}) async {
    final result = await _channel.invokeMethod('printWebView', {
      'url': url,
      "html": html,
      'printerAddress': printerAddress,
      'topOffset': topOffset
    });
    print(result);
  }

  static Future<List<PrinterBluetoothDevice>>
      getBluetoothPairedDevices() async {
    List devices = await _channel.invokeMethod('getPairedBluetoothDevices');

    return devices.map((e) => PrinterBluetoothDevice.fromJson(e)).toList();
  }

  static Future openBluetoothSetting() async {
    await _channel.invokeMethod('openBluetoothSettings');
  }

  static Future<String> testPlugin() async {
    return await _channel.invokeMethod('pluginTest');
  }
}
