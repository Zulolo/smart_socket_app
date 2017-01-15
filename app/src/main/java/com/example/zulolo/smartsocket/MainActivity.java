package com.example.zulolo.smartsocket;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import com.example.zulolo.smartsocket.types.IEsptouchListener;
import com.example.zulolo.smartsocket.types.IEsptouchResult;
import com.example.zulolo.smartsocket.types.IEsptouchTask;

import java.util.List;

public class MainActivity extends AppCompatActivity
{
    private EditText mApSsidConnected;
    private EditText mApPassword;
    private EspWifiAdminSimple mWifiAdmin;
    private Switch mSwitchIsSsidHidden;
    private Spinner mSpinnerTaskCount;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mWifiAdmin = new EspWifiAdminSimple(this);
        mApSsidConnected = (EditText)findViewById(R.id.edtApSsidConnected);
        mApPassword = (EditText)findViewById(R.id.edtApPassword);
        mSwitchIsSsidHidden = (Switch) findViewById(R.id.switchIsSsidHidden);
        mSpinnerTaskCount = (Spinner) findViewById(R.id.spinnerTaskResultCount);
//        initSpinner();
    }

    private void initSpinner()
    {
        int[] spinnerItemsInt = getResources().getIntArray(R.array.taskResultCount);
        int length = spinnerItemsInt.length;
        Integer[] spinnerItemsInteger = new Integer[length];
        for(int i=0;i<length;i++){
            spinnerItemsInteger[i] = spinnerItemsInt[i];
        }
        ArrayAdapter<Integer> adapter = new ArrayAdapter<Integer>(this,
                android.R.layout.simple_list_item_1, spinnerItemsInteger);
        mSpinnerTaskCount.setAdapter(adapter);
        mSpinnerTaskCount.setSelection(1);
    }

    /** Called when the user clicks the Send button */
    public void startSmartConfig(View view)
    {
        String apSsid = mApSsidConnected.getText().toString();
        String apPassword = mApPassword.getText().toString();
        String apBssid = mWifiAdmin.getWifiConnectedBssid();
        Boolean isSsidHidden = mSwitchIsSsidHidden.isChecked();
        String taskResultCountStr = "1";    //Integer.toString(mSpinnerTaskCount.getSelectedItemPosition());
        String isSsidHiddenStr = "NO";
        if (isSsidHidden){
            isSsidHiddenStr = "YES";
        }
        new EsptouchAsyncTask().execute(apSsid, apBssid, apPassword,
                isSsidHiddenStr, taskResultCountStr);
    }

    private void onEsptoucResultAddedPerform(final IEsptouchResult result)
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String text = result.getBssid() + " is connected to the wifi";
                Toast.makeText(MainActivity.this, text, Toast.LENGTH_LONG).show();
            }
        });
    }

    private IEsptouchListener myListener = new IEsptouchListener()
    {
        @Override
        public void onEsptouchResultAdded(final IEsptouchResult result) {
            onEsptoucResultAddedPerform(result);
        }
    };

    private class EsptouchAsyncTask extends AsyncTask<String, Void, List<IEsptouchResult>> {
        private ProgressDialog mProgressDialog;
        private IEsptouchTask mEsptouchTask;
        // without the lock, if the user tap confirm and cancel quickly enough,
        // the bug will arise. the reason is follows:
        // 0. task is starting created, but not finished
        // 1. the task is cancel for the task hasn't been created, it do nothing
        // 2. task is created
        // 3. Oops, the task should be cancelled, but it is running
        private final Object mLock = new Object();

        @Override
        protected void onPreExecute() {
            mProgressDialog = new ProgressDialog(MainActivity.this);
            mProgressDialog.setMessage("Esptouch is configuring, please wait for a moment...");
            mProgressDialog.setCanceledOnTouchOutside(false);
            mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    synchronized (mLock) {
                        if (mEsptouchTask != null) {
                            mEsptouchTask.interrupt();
                        }
                    }
                }
            });
            mProgressDialog.setButton(DialogInterface.BUTTON_POSITIVE,
                    "Waiting...", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    });
            mProgressDialog.show();
            mProgressDialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
        }

        @Override
        protected List<IEsptouchResult> doInBackground(String... params) {
            int taskResultCount = -1;
            synchronized (mLock) {
                // !!!NOTICE
                String apSsid = mWifiAdmin.getWifiConnectedSsidAscii(params[0]);
                String apBssid = params[1];
                String apPassword = params[2];
                String isSsidHiddenStr = params[3];
                String taskResultCountStr = params[4];
                boolean isSsidHidden = false;
                if (isSsidHiddenStr.equals("YES")) {
                    isSsidHidden = true;
                }
                taskResultCount = Integer.parseInt(taskResultCountStr);
                mEsptouchTask = new EsptouchTask(apSsid, apBssid, apPassword,
                        isSsidHidden, MainActivity.this);
                mEsptouchTask.setEsptouchListener(myListener);
            }
            List<IEsptouchResult> resultList = mEsptouchTask.executeForResults(taskResultCount);
            return resultList;
        }

        @Override
        protected void onPostExecute(List<IEsptouchResult> result) {
            mProgressDialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(true);
            mProgressDialog.getButton(DialogInterface.BUTTON_POSITIVE).setText("Confirm");
            IEsptouchResult firstResult = result.get(0);
            // check whether the task is cancelled and no results received
            if (!firstResult.isCancelled()) {
                int count = 0;
                // max results to be displayed, if it is more than maxDisplayCount,
                // just show the count of redundant ones
                final int maxDisplayCount = 5;
                // the task received some results including cancelled while
                // executing before receiving enough results
                if (firstResult.isSuc()) {
                    StringBuilder sb = new StringBuilder();
                    for (IEsptouchResult resultInList : result) {
                        sb.append("Esptouch success, bssid = "
                                + resultInList.getBssid()
                                + ",InetAddress = "
                                + resultInList.getInetAddress()
                                .getHostAddress() + "\n");
                        count++;
                        if (count >= maxDisplayCount) {
                            break;
                        }
                    }
                    if (count < result.size()) {
                        sb.append("\nthere's " + (result.size() - count)
                                + " more result(s) without showing\n");
                    }
                    mProgressDialog.setMessage(sb.toString());
                } else {
                    mProgressDialog.setMessage("Esptouch fail");
                }
            }
        }
    }
}
