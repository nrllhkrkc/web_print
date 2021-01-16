import 'dart:async';
import 'dart:convert';

import 'package:flutter/services.dart';
import 'package:web_print/models/PrinterBluetoothDevice.dart';

class WebPrint {
  static const MethodChannel _channel = const MethodChannel('web_print');

  static Future printWebUrl(String url) async {
    await _channel.invokeMethod('printWebUrl', {'url': url});
  }

  static Future<List<PrinterBluetoothDevice>>
      getBluetoothPairedDevices() async {
    final result = await _channel.invokeMethod('getPairedBluetoothDevices');
    var map = jsonDecode(result);

    return List.generate(
        map.length, (index) => PrinterBluetoothDevice.fromJson(map[index]));
  }

  static Future openBluetoothSetting() async {
    await _channel.invokeMethod('openBluetoothSettings');
  }
}
