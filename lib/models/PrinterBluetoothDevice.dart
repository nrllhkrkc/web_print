class PrinterBluetoothDevice {
  String name;
  String address;

  PrinterBluetoothDevice.fromJson(Map json)
      : name = json['name'],
        address = json['address'];
}
