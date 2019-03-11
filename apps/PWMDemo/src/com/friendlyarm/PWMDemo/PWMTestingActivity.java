package com.friendlyarm.PWMDemo;

import com.friendlyarm.FriendlyThings.HardwareControler;
import com.friendlyarm.FriendlyThings.BoardType;
import com.friendlyarm.PWMDemo.R;
import com.friendlyarm.Utils.CommonFuncs;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.widget.TextView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import java.io.BufferedWriter;
import android.os.Message;
import android.os.Handler;
import android.os.Looper;
import java.io.*;

public class PWMTestingActivity extends Activity {
    /** Called when the activity is first created. */
    private int mBoardType;
    private boolean mBoardSupported = true;
    private static final String TAG = "PWMDemo";

    private boolean mEnable = true;
    private TextView mTextViewFreq, mTextViewCycle;
    private SeekBar mSeekBarFreq, mSeekBarCycle;
    private CheckBox mCheckBoxEnable;
    private Button mApplyButton;
    MessageHandler mMessageHandler;

    int mFreq=20;  //KHz
    int mCycle=90;    //0~100

    private class MessageHandler extends Handler {
        Activity parent;
        public MessageHandler(Looper looper, Activity v) {
            super(looper);
            parent = v;
        }
        
        @Override
        public void handleMessage(Message msg) {
            String result = (String) msg.obj;
            if (result.equals("START")) {
                Toast.makeText(parent, String.format("Please wait..."),
                    Toast.LENGTH_SHORT).show();
                enableUI(false);
            } else if (result.equals("DONE")) {
                Toast.makeText(parent, String.format("Done"),
                    Toast.LENGTH_SHORT).show();
                enableUI(true);
            } else {
                Toast.makeText(parent, result,
                    Toast.LENGTH_SHORT).show();
            }
        }
    }
    private void sendMessage(String msg) {
        Message message = Message.obtain();
        message.obj = msg;
        mMessageHandler.sendMessage(message);
    }

    private void enableUI(boolean b) {
        mSeekBarFreq.setEnabled(b);
        mSeekBarCycle.setEnabled(b);
        mCheckBoxEnable.setEnabled(b);
        mApplyButton.setEnabled(b);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pwmtestingactivity);
        Looper looper = Looper.myLooper();
        mMessageHandler = new MessageHandler(looper, this);

        mTextViewFreq = (TextView)findViewById(R.id.text_freq);
        mTextViewCycle = (TextView)findViewById(R.id.text_cycle);
        mSeekBarFreq = (SeekBar)findViewById(R.id.seekbar_pwm_freq);
        mSeekBarCycle = (SeekBar)findViewById(R.id.seekbar_cycle);
        mCheckBoxEnable = (CheckBox)findViewById(R.id.checkbox_enable);
        mApplyButton = (Button)findViewById(R.id.button_apply);
        
        mSeekBarFreq.setProgress(mFreq);
        mSeekBarCycle.setProgress(mCycle);
        mCheckBoxEnable.setChecked(mEnable);

        mSeekBarFreq.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if (i > 0) {
                    mFreq = i;
                }
                setGuiText();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        mSeekBarCycle.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                mCycle = i;
                setGuiText();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        setGuiText();

        mBoardType = HardwareControler.getBoardType();
        if (mBoardType == BoardType.NanoPC_T4 || mBoardType == BoardType.NanoPi_M4 || mBoardType == BoardType.NanoPi_NEO4) {
        } else {
            Toast.makeText(this, String.format("Current implementation only supports NanoPC-T4, NanoPi-M4 and NanoPi-NEO4."),
                Toast.LENGTH_SHORT).show();
            mBoardSupported = false;
        }
        applyPWMSetting();
    }

    void writeNumToFile(String fileName, long v) {
        Log.d(TAG, "#### Write " + Long.toString(v) + " to " + fileName);
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
            writer.write(Long.toString(v));
            writer.close();
        } catch(IOException ie) {
            ie.printStackTrace();
            Log.e(TAG, "Write file error: " + fileName);
            Log.e(TAG, ie.toString());
            sendMessage("Fail to write: " + fileName + "(value: " + Long.toString(v) + ")");
        } 
    }

    private void applyPWMSetting() {
        if (mBoardSupported) {
            new Thread() {
                @Override
                public void run() {
                    try {
                        sendMessage("START");
                        File file = new File("/sys/class/pwm/pwmchip1/pwm0/enable");
                        if (file.exists()) {
                            writeNumToFile("/sys/class/pwm/pwmchip1/pwm0/enable", 0);
                            Thread.sleep(100);
                        } else {
                            writeNumToFile("/sys/class/pwm/pwmchip1/export", 0);
                            Thread.sleep(1000);
                        }

                        if (mEnable) {
                            long period = 1000000000/(mFreq*1000);
                            writeNumToFile("/sys/class/pwm/pwmchip1/pwm0/period", period);
                            Thread.sleep(100);
                            writeNumToFile("/sys/class/pwm/pwmchip1/pwm0/duty_cycle", (long)(period * (mCycle/100.0)));
                            Thread.sleep(100);
                            writeNumToFile("/sys/class/pwm/pwmchip1/pwm0/enable", 1);
                            Thread.sleep(100);
                        }

                        sendMessage("DONE");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }.start();
        }
    }

    private void setGuiText(){
        mTextViewFreq.setText(getString(R.string.txtFreq) + " (" + String.valueOf(mFreq) + "KHz)");
        mTextViewCycle.setText(getString(R.string.txtDutyCycle) + " (" + String.valueOf(mCycle) + "%)");
    }

    @Override
    public void onDestroy() {
        if (mBoardSupported) {
            writeNumToFile("/sys/class/pwm/pwmchip1/pwm0/enable", 0);
            try {
                Thread.sleep(100,0);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            writeNumToFile("/sys/class/pwm/pwmchip1/unexport", 0);
        }
        super.onDestroy();
    }

    public void onApplyPressed(View view) {
        if (!mBoardSupported) {
            Toast.makeText(this, String.format("Current implementation only supports NanoPC-T4, NanoPi-M4 and NanoPi-NEO4."),
                Toast.LENGTH_SHORT).show();
            return ;
        }
        applyPWMSetting();
    }

    public void onCheckboxEnableClicked(View view) {
        mEnable = ((CheckBox)view).isChecked();
    }
}
