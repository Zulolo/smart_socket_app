package com.example.zulolo.smartsocket;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

public class MainActivity extends AppCompatActivity {
    private EditText mApSsidConnected;
    private EditText mApPassword;
    private EspWifiAdminSimple mWifiAdmin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mApSsidConnected = (EditText)findViewById(R.id.edtApSsidConnected);
        mApPassword = (EditText)findViewById(R.id.edtApPassword);
    }

    /** Called when the user clicks the Send button */
    public void startSmartConfig(View view) {

    }
}
