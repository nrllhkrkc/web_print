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
  String? address;
  late int topOffset;

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
          child: Column(
            children: [
              Padding(
                padding: const EdgeInsets.all(8.0),
                child: TextFormField(
                  onFieldSubmitted: (value) {
                    topOffset = int.tryParse(value)!;
                  },
                  keyboardType: TextInputType.number,
                  decoration: InputDecoration(labelText: "Top Offset"),
                ),
              ),
              ListView.builder(
                  shrinkWrap: true,
                  itemCount: _pairedList.length,
                  itemBuilder: (_, index) => ListTile(
                        onTap: () {
                          address = _pairedList[index].address;
                          setState(() {});
                        },
                        title: Text(_pairedList[index].name),
                        trailing: _pairedList[index].address == address
                            ? Icon(Icons.check)
                            : null,
                      )),
            ],
          ),
        ),
        floatingActionButton: FloatingActionButton(
          onPressed: () {
            WebPrint.printWebView(
                html: "<html><body><h3>Fuck you</h3>Kes lan</body></html>",
                printerAddress: address!,
                topOffset: topOffset);
          },
          child: Icon(Icons.print),
        ),
      ),
    );
  }
}
