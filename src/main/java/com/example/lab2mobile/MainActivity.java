package com.example.lab2mobile;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.method.ScrollingMovementMethod;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.maps.SupportMapFragment;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private EditText nameInput, addressInput, amountOfStudents, webometricsRate, excellenceRate;
    private TextView resultText;
    private DBHelper dbHelper;
    private MapHandler mapHandler;
    private Spinner contactSpinner;
    private final List<Pair<String, String>> contactList = new ArrayList<>();

    private static final int PERMISSIONS_REQUEST_READ_CONTACTS = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        contactSpinner = findViewById(R.id.countrySpinner);

        checkPermissions(false);

        nameInput = findViewById(R.id.nameInput);
        addressInput = findViewById(R.id.city);
        amountOfStudents = findViewById(R.id.amountOfStudents);
        webometricsRate = findViewById(R.id.rating);
        excellenceRate = findViewById(R.id.excellenceRate);
        resultText = findViewById(R.id.resultText);
        resultText.setMovementMethod(new ScrollingMovementMethod());

        dbHelper = new DBHelper(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapHandler = new MapHandler(mapFragment, this);
        }
        loadUniversities();
    }

    private void loadContacts() {
        String[] projection = {
                ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS
        };

        Cursor cursor = getContentResolver().query(
                ContactsContract.Data.CONTENT_URI,
                projection,
                ContactsContract.Data.MIMETYPE + " = ?",
                new String[]{ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE},
                null
        );

        if (cursor != null && cursor.moveToFirst()) {
            do {
                String name = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME));
                String address = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS));

                if (address != null && !address.isEmpty()) {
                    contactList.add(new Pair<>(name, address));
                }
            } while (cursor.moveToNext());
            cursor.close();
        }
    }

    public void showRoute(View view) {
        int selectedIndex = contactSpinner.getSelectedItemPosition();
        if (selectedIndex >= 0) {
            String selectedItem = (String) contactSpinner.getSelectedItem();
            String[] parts = selectedItem.split(" - ");
            if (parts.length == 2) {
                mapHandler.showRoute(parts[1]);
            } else {
                Toast.makeText(this, "Invalid format", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "No university selected", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Додаємо меню в ActionBar
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_about) {
            Intent intent = new Intent(this, AboutActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void addUniversity(View view) {
        try {
            String fullName = nameInput.getText().toString();
            String address = addressInput.getText().toString();
            int students = Integer.parseInt(amountOfStudents.getText().toString());
            int webometrics = Integer.parseInt(webometricsRate.getText().toString());
            int excellence = Integer.parseInt(excellenceRate.getText().toString());

            SQLiteDatabase db = dbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put("fullname", fullName);
            values.put("address", address);
            values.put("amountOfStudents", students);
            values.put("webometricsRate", webometrics);
            values.put("excellenceRate", excellence);

            long newRowId = db.insert("universities", null, values);
            if (newRowId != -1) {
                Toast.makeText(this, "University added successfully", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();

        }
        loadUniversities();
    }

    public void showFilteredUniversities(View view) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;

        try {
            cursor = db.rawQuery("SELECT fullname, address, excellenceRate FROM universities " +
                    "WHERE address NOT LIKE '%Київ%' AND excellenceRate < 5000", null);

            StringBuilder result = new StringBuilder();
            if (cursor.moveToFirst()) {
                do {
                    String name = cursor.getString(cursor.getColumnIndexOrThrow("fullname"));
                    String address = cursor.getString(cursor.getColumnIndexOrThrow("address"));
                    String excellence = cursor.getString(cursor.getColumnIndexOrThrow("excellenceRate"));
                    result.append(name).append(" - ").append(address).append(" - ").append(excellence).append("\n");
                } while (cursor.moveToNext());
            }
            resultText.setText(result.toString());
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    public void showWebometricsStats(View view) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        try (Cursor cursor = db.rawQuery(
                "SELECT " +
                        "(SELECT fullname || ', ' || address || ', ' || webometricsRate " +
                        "FROM universities " +
                        "WHERE webometricsRate = (SELECT MAX(webometricsRate) FROM universities)) AS maxInfo, " +
                        "(SELECT fullname || ', ' || address || ', ' || webometricsRate " +
                        "FROM universities " +
                        "WHERE webometricsRate = (SELECT MIN(webometricsRate) FROM universities)) AS minInfo",
                null)) {

            if (cursor.moveToFirst()) {
                String maxInfo = cursor.getString(cursor.getColumnIndexOrThrow("maxInfo"));
                String minInfo = cursor.getString(cursor.getColumnIndexOrThrow("minInfo"));

                String result = "Max: " + maxInfo + "\n" +
                        "Min: " + minInfo;

                resultText.setText(result);
            } else {
                resultText.setText("No data found.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public void showFilteredContacts() {
        try {
            String selection = ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME + " LIKE ?";
            String[] selectionArgs = new String[]{"Т%"};

            Cursor cursor = getContentResolver().query(
                    ContactsContract.Data.CONTENT_URI,
                    new String[]{ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME},
                    selection,
                    selectionArgs,
                    null
            );

            contactList.clear();

            StringBuilder result = new StringBuilder();
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    String name = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME));

                    if (name != null && !name.isEmpty()) {
                        result.append(name).append("\n");
                    }
                } while (cursor.moveToNext());
                cursor.close();
            }

            resultText.setText(result.toString());
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public void showFilteredContacts(View view) {
        checkPermissions(true);
    }

    private void checkPermissions(boolean show) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CONTACTS}, PERMISSIONS_REQUEST_READ_CONTACTS);
        } else {
            if (show) {
                showFilteredContacts();
            } else {
                loadContacts();
            }
        }
    }

    private void loadUniversities() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;

        List<String> spinnerItems = new ArrayList<>();
        contactList.clear();

        try {
            cursor = db.rawQuery("SELECT fullname, address FROM universities", null);

            if (cursor.moveToFirst()) {
                do {
                    String name = cursor.getString(cursor.getColumnIndexOrThrow("fullname"));
                    String address = cursor.getString(cursor.getColumnIndexOrThrow("address"));

                    contactList.add(new Pair<>(name, address));
                    spinnerItems.add(name + " - " + address);
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, spinnerItems);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        contactSpinner.setAdapter(adapter);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_READ_CONTACTS && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            showFilteredContacts();
            loadContacts();
        }
    }
}
