package com.varvet.barcodereadersample

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Button
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.vision.barcode.Barcode
import com.varvet.barcodereadersample.barcode.BarcodeCaptureActivity
import java.util.*
import android.support.v4.widget.DrawerLayout
import android.widget.ListView
import android.widget.ArrayAdapter
import android.widget.AdapterView
import java.io.*
import java.util.concurrent.TimeUnit
import kotlin.collections.HashMap
import kotlin.Long



class MainActivity : AppCompatActivity() {

    private lateinit var mResultTextView: TextView
    private lateinit var entradas: HashMap<String, Date>
    private lateinit var duracion: HashMap<String, Long>
    private lateinit var lastButtonPressed: String
    private lateinit var opcionesDrawer: Array<String>
    private lateinit var mDrawerLayout: DrawerLayout
    private lateinit var mDrawerList: ListView
    var FILENAME = "duraciones.txt"
    var ACREEDORES = "acreedores.txt"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        opcionesDrawer = getResources().getStringArray(R.array.opciones_drawer);
        mDrawerLayout = findViewById(R.id.drawer_layout) as DrawerLayout;
        mDrawerList = findViewById(R.id.left_drawer) as ListView
        mResultTextView = findViewById(R.id.result_textview)
        entradas = HashMap()
        duracion = HashMap()

        // Set the adapter for the list view
        mDrawerList.setAdapter(ArrayAdapter(this, R.layout.drawer_list_item, opcionesDrawer))
        // Set the list's click listener
        mDrawerList.setOnItemClickListener(fun(parent: AdapterView<*>, view: View, position: Int, id: Long) {
            selectItem(position)
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == BARCODE_READER_REQUEST_CODE) {
            if (resultCode == CommonStatusCodes.SUCCESS) {
                if (data != null) {
                    val barcode = data.getParcelableExtra<Barcode>(BarcodeCaptureActivity.BarcodeObject)
                    val p = barcode.cornerPoints
                    if (lastButtonPressed == "Entrada") {
                        mResultTextView.text = "Hola " + barcode.displayValue
                        val current = Calendar.getInstance().time
                        entradas.put(barcode.displayValue, current)
                        Toast.makeText(this, "Se agrego " + barcode.displayValue + "\n" +
                                current.toString(), Toast.LENGTH_SHORT).show()
                    } else {
                        val oldDate = entradas.remove(barcode.displayValue)
                        if (oldDate != null) {
                            mResultTextView.text = "Adios " + barcode.displayValue
                            val newTime = Calendar.getInstance().time.time
                            val oldTime = oldDate.time
                            val diff = timeDiff(oldTime, newTime)
                            if (duracion.containsKey(barcode.displayValue)) {
                                val oldDuration = duracion.get(barcode.displayValue)
                                val newDuration = oldDuration!! + diff
                                duracion.put(barcode.displayValue, newDuration)
                            } else {
                                duracion.put(barcode.displayValue, diff)
                            }
                            Toast.makeText(this, "Se elimino " + barcode.displayValue +
                                    "\nDuracion: " + diff, Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, "Codigo invalido", Toast.LENGTH_SHORT).show()
                        }
                    }

                } else
                    mResultTextView.setText(R.string.no_barcode_captured)
            } else
                Log.e(LOG_TAG, String.format(getString(R.string.barcode_error_format),
                        CommonStatusCodes.getStatusCodeString(resultCode)))
        } else
            super.onActivityResult(requestCode, resultCode, data)
    }

    fun leerQr(view: View) {
        val intent = Intent(applicationContext, BarcodeCaptureActivity::class.java)
        var button = view as Button
        lastButtonPressed = button.text as String
        startActivityForResult(intent, BARCODE_READER_REQUEST_CODE)
    }

    fun timeDiff(oldTime: Long, newTime: Long): Long {
        val diffInMillisec = newTime - oldTime
        val diffInSec = TimeUnit.MILLISECONDS.toSeconds(diffInMillisec)
        return diffInSec
    }

    fun hashMapToArray(hash: HashMap<String, *>): Array<String?> {
        var array = arrayOfNulls<String>(hash.size)
        var i= 0
        hash.forEach { array.set(i, it.toString()); i++ }
        return array
    }

    companion object {
        private val LOG_TAG = MainActivity::class.java.simpleName
        private val BARCODE_READER_REQUEST_CODE = 1
    }

    /** Swaps fragments in the main content view  */
    private fun selectItem(position: Int) {
        when (position) {
            0 -> {
                val fm = supportFragmentManager
                val listaFragment: MyFragment = MyFragment.newInstance(opcionesDrawer[position], hashMapToArray(entradas))
                listaFragment.show(fm, opcionesDrawer[position])
            }
            1 -> {val fm = supportFragmentManager
                val listaFragment: MyFragment = MyFragment.newInstance(opcionesDrawer[position], hashMapToArray(duracion))
                listaFragment.show(fm, opcionesDrawer[position])
            }
            2 ->{
                saveHashMap(duracion, FILENAME)
            }
            3 ->{
                readDuraciones()
            }
            4 ->{
                saveHashMap(acreedoresConstancia(duracion), ACREEDORES)
            }
        }
        mDrawerLayout.closeDrawer(mDrawerList)
    }

    fun isExternalStorageWritable(): Boolean {
        return Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
    }

    /* Checks if external storage is available to at least read */
    fun isExternalStorageReadable(): Boolean {
        return Environment.getExternalStorageState() in
                setOf(Environment.MEDIA_MOUNTED, Environment.MEDIA_MOUNTED_READ_ONLY)
    }

    fun getPublicDocStorageDir(fileName: String): File? {
        // Get the directory for the user's public documents directory.
        val file = File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOCUMENTS), fileName)
        if (!file?.mkdirs()) {
            Log.e(LOG_TAG, "Directory not created")
        }
        return file
    }


    fun saveHashMap(map : HashMap<String, Long>, fileName: String) {
        if (isExternalStorageWritable()) {
            var durations = File(getPublicDocStorageDir("Documents"), fileName)
            try {
                var fos = FileOutputStream(durations)
                map.forEach { (key, value) ->
                    var linea = key + ":" + value.toString() + "\n"
                    fos.write(linea.toByteArray())
                }
                fos.close()
                Toast.makeText(this, "Guardado", Toast.LENGTH_SHORT).show()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        } else {
            Toast.makeText(this, "No se puede guardar", Toast.LENGTH_SHORT).show()
        }
    }

    fun readDuraciones() {
        if (isExternalStorageReadable()) {
            var sb = StringBuilder()
            try {
                var durations =File(getPublicDocStorageDir("Documents"), FILENAME)
                var fis = FileInputStream(durations)
                if (fis != null) {
                    var isr = InputStreamReader(fis)
                    var buff = BufferedReader(isr)
                    var linea: String?
                    linea = buff.readLine()
                    while (linea != null) {
                        sb.append(linea + "\n");
                        linea = buff.readLine()
                    }
                    fis.close()
                }
                duracion = stringToHashMap(sb.toString())
            } catch (e: IOException) {
                e.printStackTrace()
            }
        } else {
            Toast.makeText(this, "No se puede leer", Toast.LENGTH_SHORT).show()
        }
    }

    fun stringToHashMap(s: String) : HashMap<String, Long>{
        var hash = hashMapOf<String, Long>()
        var split = s.split('\n')
        split.forEach { s ->
            var pair = s.split(":")
            println(s)
            if(pair.size == 2)
                hash[pair[0]] = pair[1].toLong()
         }
        return hash
    }

    //Solo los participantes con 70% de asistencia o mas reciben constancia
    fun acreedoresConstancia(map: HashMap<String, Long>) : HashMap<String, Long> {
        var hash = hashMapOf<String, Long>()
        map.forEach{(key, value) ->
            //35280
            if(value >= 35280){
                hash[key] = value
            }
        }
        return hash
    }
}
