package br.com.tauasanto.kotlinbluetooth

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.os.Handler
import android.widget.Button
import android.widget.Toast
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.lang.Exception
import java.util.*

class MainActivity : AppCompatActivity() {
    private var btAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private var btSocket: BluetoothSocket? = null
    var activar = false
    var bluetoothIn: Handler? = null
    val handlerState = 0
    private var MyConexionBT: ConnectedThread? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val acenderR1: Button = findViewById(R.id.acenderR1)
        val acenderR2: Button = findViewById(R.id.acenderR2)
        val acenderL1: Button = findViewById(R.id.acenderL1)
        val acenderL2: Button = findViewById(R.id.acenderL2)
        val conectar: Button = findViewById(R.id.conectar)
        val desligar: Button = findViewById(R.id.apagar)
        val desconectar: Button = findViewById(R.id.Desconectar)
        verificarBluetooth()
        val pairedDeveicesList = btAdapter.bondedDevices

        if (pairedDeveicesList != null) {
            for (pairedDevice in pairedDeveicesList) {
                if (pairedDevice.name == "HC-05") {
                    address = pairedDevice.address
                }
            }
        }
        conectar.setOnClickListener {
            activar = true
            onResume()
        }
        acenderR1.setOnClickListener { MyConexionBT?.write("1") }
        acenderR2.setOnClickListener { MyConexionBT?.write("2") }
        acenderL1.setOnClickListener{ MyConexionBT?.write("3") }
        acenderL2.setOnClickListener{ MyConexionBT?.write("4") }
        desligar.setOnClickListener{ MyConexionBT?.write("0") }
        desconectar.setOnClickListener{
            try {
                btSocket!!.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    @Throws(IOException::class)
    private fun createBluetoothSocket(device: BluetoothDevice): BluetoothSocket {
        return device.createRfcommSocketToServiceRecord(BTMODULEUUID)
    }

    private fun verificarBluetooth() {
        if (!btAdapter.isEnabled) {
            val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(intent, 1)
        }
    }

    public override fun onResume() {
        super.onResume()
        if (activar) {
            val device = btAdapter.getRemoteDevice(address)

            try {
            btSocket = createBluetoothSocket(device);
            } catch (e: IOException) {
                Toast.makeText(getBaseContext(), "A conexão com o Socket falhou", Toast.LENGTH_LONG).show();
            }
            try {
                btSocket!!.connect()
            } catch (e: IOException) {
                try {
                    btSocket!!.close()
                } catch (e2: IOException) {
                }
            }
            MyConexionBT = ConnectedThread(btSocket)
            MyConexionBT?.start()
        }
    }

    private inner class ConnectedThread(socket: BluetoothSocket?) : Thread() {
        private val mmInStream: InputStream?
        private val mmOutStream: OutputStream?
        override fun run() {
            val buffer = ByteArray(256)
            var bytes: Int

            while (true) {
                try {
                    bytes = mmInStream!!.read(buffer)
                    val readMessage = String(buffer, 0, bytes)
                    bluetoothIn!!.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget()
                } catch (e: IOException) {
                    break
                }
            }
        }

        fun write(input: String) {
            try {
                mmOutStream!!.write(input.toByteArray())
            } catch (e: IOException) {
                Toast.makeText(baseContext, "A conexão falhou", Toast.LENGTH_LONG).show()
                finish()
            }
        }

        init {
            var tmpIn: InputStream? = null
            var tmpOut: OutputStream? = null
            try {
                if (socket != null) {
                    tmpIn = socket.inputStream
                    tmpOut = socket.outputStream
                }
            } catch (e: IOException) {
            }
            mmInStream = tmpIn
            mmOutStream = tmpOut
        }
    }
    companion object {
        var address: String? = null
        private val BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }
}