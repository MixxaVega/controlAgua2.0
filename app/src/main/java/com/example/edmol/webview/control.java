package com.example.edmol.webview;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.TabLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class control extends AppCompatActivity implements TextWatcher {
    RelativeLayout display2, auto_mood, manual_mood;
    TextView aguaTotal;
    TextView flujoAgua;
    TextView equivalencia;
    ToggleButton btnModo;
    String txtCantidad;
    RadioButton rbGalones,rMC;
    TextView textView1;
    ImageView regresar;

    //variables para la conexion
    Handler bluetoothIn;
    EditText cantidad;
    ToggleButton botonPrender3, botonManual;
    final int handlerState = 0;                         //used to identify handler message
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private StringBuilder recDataString = new StringBuilder();
    final AdminBD bd = new AdminBD(this);
    private ConnectedThread mConnectedThread;

    // SPP UUID service - this should work for most devices
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control);
        textView1 = (TextView) findViewById(R.id.tvParametros);
        cantidad = findViewById(R.id.etAgua);
        botonManual = findViewById(R.id.botonPrender);
        display2 = (RelativeLayout) findViewById(R.id.Fondo);
        auto_mood = (RelativeLayout) findViewById(R.id.rlAutomatico);
        manual_mood = (RelativeLayout) findViewById(R.id.rlManual);
        aguaTotal = (TextView) findViewById(R.id.txtCantidad);
        flujoAgua = (TextView) findViewById(R.id.txtFlujo);
        btnModo = (ToggleButton) findViewById(R.id.btnMood);
        botonPrender3 = (ToggleButton) findViewById(R.id.botonPrender3);
        equivalencia = (TextView) findViewById(R.id.txtEqui);
        rbGalones = (RadioButton) findViewById(R.id.rbGalones);
        rMC = findViewById(R.id.rbCubicos);
        regresar=findViewById(R.id.regresarMedidor);
        regresar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch(v.getId()){
                    case R.id.regresarMedidor:
                        botonPrender3.setChecked(false);
                        botonManual.setChecked(false);
                        mConnectedThread.write("x");
                        finish();
                        break;

                }
            }
        });

        aguaTotal.addTextChangedListener(this);


        String fondoActual, tamanoActual;
        Intent i =getIntent();
        if(i.hasExtra("fondoSeleccionado")){
            fondoActual = i.getExtras().getString("fondoActual");
            switch (fondoActual) {
                case "colorFondo1":
                    display2.setBackgroundColor(getResources().getColor(R.color.colorFondo1));
                    break;
                case "colorFondo2":
                    display2.setBackgroundColor(getResources().getColor(R.color.colorFondo2));
                    break;
                case "colorFondo3":
                    display2.setBackgroundColor(getResources().getColor(R.color.colorFondo3));
                    break;
                case "colorFondo4":
                    display2.setBackgroundColor(getResources().getColor(R.color.colorFondo4));
                    break;
                default:
                    Toast.makeText(this, "Algo malo pasó", Toast.LENGTH_SHORT).show();
                    break;
            }

        }
        //fondoActual = getIntent().getExtras().getString("fondoActual");

       // tamanoActual = getIntent().getExtras().getString("tamanoActual");
        if(i.hasExtra("tamanoSeleccionado")){
            tamanoActual = i.getExtras().getString("tamanoActual");

            switch (tamanoActual) {
                case "tamano25":
                    textView1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
                    break;
                case "tamano30":
                    textView1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 25);
                    break;
                case "tamano35":
                    textView1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28);
                    break;
                default:
                    break;
            }

        }



        btnModo.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    auto_mood.setVisibility(CompoundButton.INVISIBLE);
                    manual_mood.setVisibility(CompoundButton.VISIBLE);
                    botonPrender3.setChecked(false);
                } else {
                    auto_mood.setVisibility(CompoundButton.VISIBLE);
                    manual_mood.setVisibility(CompoundButton.INVISIBLE);
                    botonManual.setChecked(false);
                }
            }
        });

        //codigo de conexion
        bluetoothIn = new Handler() {
            public void handleMessage(android.os.Message msg) {
                if (msg.what == handlerState) {                                        //if message is what we want
                    String readMessage = (String) msg.obj;                                                                // msg.arg1 = bytes from connect thread
                    recDataString.append(readMessage);                                    //keep appending to string until ~
                    int endOfLineIndex = recDataString.indexOf("~");                    // determine the end-of-line
                    if (endOfLineIndex > 0) {                                           // make sure there data before ~
                        String dataInPrint = recDataString.substring(0, endOfLineIndex);    // extract string
                        //flujoAgua.setText("Datos recibidos = " + dataInPrint);
                        int dataLength = dataInPrint.length();                            //get length of data received
                        //txtStringLength.setText("Tamaño del String = " + String.valueOf(dataLength));
                        String[] datos = dataInPrint.split("\\+");

                        if (recDataString.charAt(0) == '#')                                //if it starts with # we know it is what we are looking for
                        {
                            //separa el string recibido
                            String valor0 = recDataString.substring(1, 5);             //get sensor value from string between indices 1-5
                            String valor1 = recDataString.substring(6, 10);            //same again...
                            //String sensor2 = recDataString.substring(11, 15);
                            //String sensor3 = recDataString.substring(16, 20);

                            flujoAgua.setText(datos[0]); //se insertan los valores que manda el arduino
                            aguaTotal.setText(datos[1]);

                            /*if(sensor0.equals("1.00"))
                                sensorView0.setText("Encendido");	//update the textviews with sensor values
                            else
                                sensorView0.setText("Apagado");	//update the textviews with sensor values
                                sensorView1.setText(sensor1);
                                sensorView2.setText(sensor2);
                                sensorView3.setText(sensor3);
                            //sensorView3.setText(" Sensor 3 Voltage = " + sensor3 + "V");
                            */
                        }
                        recDataString.delete(0, recDataString.length());                    //clear all string data
                        // strIncom =" ";
                        dataInPrint = " ";
                    }
                }
            }
        };

        btAdapter = BluetoothAdapter.getDefaultAdapter();       // get Bluetooth adapter
        checkBTState();
        botonManual.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(!b){
                    mConnectedThread.write("x");
                    //INSERTAR BD MANUAL
                    if(!txtCantidad.isEmpty()&&btnModo.isChecked()) {
                   SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.getDefault());
                    Date date = new Date();
                    String fecha = dateFormat.format(date);
                    String txtCaudal = flujoAgua.getText().toString();
                    SQLiteDatabase basedatos = bd.getWritableDatabase();
                    bd.insertarRegistro(basedatos,fecha,Float.parseFloat(txtCantidad),"manual",txtCaudal);}
                }else{
                    mConnectedThread.write("o");
                }
            }
        });

        botonPrender3.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                txtCantidad = cantidad.getText().toString();
                if (!isChecked) {
                    cantidad.setEnabled(true);
                    mConnectedThread.write("x");
                    //INSERTAR BD AUTOMATICO
                   /* if(!txtCantidad.isEmpty()) {
                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.getDefault());
                        Date date = new Date();
                        String fecha = dateFormat.format(date);
                        String txtCaudal = flujoAgua.getText().toString();
                        SQLiteDatabase basedatos = bd.getWritableDatabase();
                        bd.insertarRegistro(basedatos, fecha, Float.parseFloat(txtCantidad), "automatico", txtCaudal);
                    }*/
                }

                else {
                    if( txtCantidad.isEmpty()){
                        botonPrender3.setChecked(false);
                        AlertDialog alertDialog = new AlertDialog.Builder(control.this).create();
                        alertDialog.setTitle("Alerta");
                        alertDialog.setMessage("Introduce una cantidad para comenzar la medición ");
                        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                });
                        alertDialog.show();
                    }else{
                        mConnectedThread.write("o");
                        cantidad.setEnabled(false);
                        if(rbGalones.isChecked()){
                            String s = cantidad.getText().toString();
                            Float galones = Float.parseFloat(s);
                            double litros = galones / 3.78;
                            equivalencia.setText("Equivale a "+Double.toString(litros)+" litros");
                        }else{
                            if(rMC.isChecked()){
                                String s = cantidad.getText().toString();
                                Float mC = Float.parseFloat(s);
                                double litros = mC / 1000;
                                equivalencia.setText("Equivale a "+Double.toString(litros)+" litros");
                            }
                        }

                    }


                    //manda x cuando se prende
                    //mConnectedThread.write(agua.getText().toString()); //manda cantidad de agua
                }
                //manda o cuando se apaga

            }
        });




        //EJEMPLO DE APAGAR, ENCENDER Y MANDAR VALOR AL ARDUINO
        /*// Set up onClick listeners for buttons to send 1 or 0 to turn on/off LED
        btnOff.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mConnectedThread.write("x");    // Send "0" via Bluetooth
                Toast.makeText(getBaseContext(), "Apagar el LED", Toast.LENGTH_SHORT).show();
            }
        });

        btnOn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mConnectedThread.write("o");    // Send "1" via Bluetooth
                Toast.makeText(getBaseContext(), "Encender el LED", Toast.LENGTH_SHORT).show();
            }
        });
        //mandar velocidad de 0 a 9
        btnAceptar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                mConnectedThread.write(velocidad.getText().toString());    // Send "0-9" via Bluetooth
                Toast.makeText(getBaseContext(), "Manda velocidad", Toast.LENGTH_SHORT).show();

            }
        });*/
    }
    private void parar( ){

    }

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {

        return device.createRfcommSocketToServiceRecord(BTMODULEUUID);
        //creates secure outgoing connecetion with BT device using UUID
    }

    @Override
    public void onResume() {
        super.onResume();

        //create device and set the MAC address
        BluetoothDevice device = btAdapter.getRemoteDevice("98:D3:32:30:77:71");

        try {
            btSocket = createBluetoothSocket(device);
        } catch (IOException e) {
            Toast.makeText(this, "La creacción del Socket fallo", Toast.LENGTH_LONG).show();
        }
        // Establish the Bluetooth socket connection.
        try {
            btSocket.connect();
        } catch (IOException e) {
            try {
                btSocket.close();
            } catch (IOException e2) {
                //insert code to deal with this
            }
        }
        mConnectedThread = new ConnectedThread(btSocket);
        mConnectedThread.start();

        //I send a character when resuming.beginning transmission to check device is connected
        //If it is not an exception will be thrown in the write method and finish() will be called
        mConnectedThread.write("x");
    }

    @Override
    public void onPause() {
        super.onPause();
        try {
            //Don't leave Bluetooth sockets open when leaving activity
            btSocket.close();
        } catch (IOException e2) {
            //insert code to deal with this
        }
    }

    //Checks that the Android device Bluetooth is available and prompts to be turned on if off
    private void checkBTState() {

        if (btAdapter == null) {
            Toast.makeText(this, "El dispositivo no soporta bluetooth", Toast.LENGTH_LONG).show();
        } else {
            if (btAdapter.isEnabled()) {
            } else {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
            }
        }
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        txtCantidad = cantidad.getText().toString();
        if (txtCantidad.isEmpty()) {

        } else {
            if (Float.parseFloat(charSequence.toString()) >= Float.parseFloat(txtCantidad)) {
                cantidad.setEnabled(true);
                mConnectedThread.write("x");
                botonPrender3.setChecked(false);
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.getDefault());
                Date date = new Date();
                String fecha = dateFormat.format(date);
                String txtCaudal = flujoAgua.getText().toString();
                SQLiteDatabase basedatos = bd.getWritableDatabase();
                bd.insertarRegistro(basedatos,fecha,Float.parseFloat(txtCantidad),"automatico",txtCaudal);
            }
        }
    }

    @Override
    public void afterTextChanged(Editable editable) {

    }

    //create new class for connect thread
    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        //creation of the connect thread
        public ConnectedThread(BluetoothSocket socket) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                //Create I/O streams for connection
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }


        public void run() {
            byte[] buffer = new byte[256];
            int bytes;

            // Keep looping to listen for received messages
            while (true) {
                try {
                    bytes = mmInStream.read(buffer);            //read bytes from input buffer
                    String readMessage = new String(buffer, 0, bytes);
                    // Send the obtained bytes to the UI Activity via handler
                    bluetoothIn.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget();
                } catch (IOException e) {
                    break;
                }
            }
        }

        //write method
        public void write(String input) {
            byte[] msgBuffer = input.getBytes();           //converts entered String into bytes
            try {
                mmOutStream.write(msgBuffer);                //write bytes over BT connection via outstream
            } catch (IOException e) {
                //if you cannot write, close the application
               // Toast.makeText(control.this, "La Conexión fallo", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }
}