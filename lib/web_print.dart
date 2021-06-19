import 'dart:async';

import 'package:flutter/services.dart';
import 'package:web_print/models/PrinterBluetoothDevice.dart';

class WebPrint {
  static const MethodChannel _channel = const MethodChannel('web_print');

  static Future printWebUrl(String url,
      {required String printerAddress, int? topOffset}) async {
    await _channel.invokeMethod('printWebUrl',
        {'url': url, 'printerAddress': printerAddress, 'topOffset': topOffset});
  }

  static Future<List<PrinterBluetoothDevice>>
      getBluetoothPairedDevices() async {
    List devices = await _channel.invokeMethod('getPairedBluetoothDevices');

    return devices.map((e) => PrinterBluetoothDevice.fromJson(e)).toList();
  }

  static Future openBluetoothSetting() async {
    await _channel.invokeMethod('openBluetoothSettings');
  }
}
