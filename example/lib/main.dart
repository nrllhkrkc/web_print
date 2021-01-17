import 'package:flutter/material.dart';
import 'package:web_print/models/PrinterBluetoothDevice.dart';
import 'package:web_print/web_print.dart';

void main() {
  runApp(MyApp());
}

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  List<PrinterBluetoothDevice> _pairedList = [];
  String address;

  @override
  void initState() {
    super.initState();
    WebPrint.getBluetoothPairedDevices().then((value) {
      setState(() {
        _pairedList = value;
      });
    });
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: Center(
          child: ListView.builder(
              itemCount: _pairedList.length,
              itemBuilder: (_, index) => ListTile(
                    onTap: () {
                      address = _pairedList[index].address;
                    },
                    title: Text(_pairedList[index].name),
                  )),
        ),
        floatingActionButton: FloatingActionButton(
          onPressed: () {
            WebPrint.printWebUrl("https://akuple.com/fatura",
                printerAddress: address);
          },
          child: Icon(Icons.print),
        ),
      ),
    );
  }
}
