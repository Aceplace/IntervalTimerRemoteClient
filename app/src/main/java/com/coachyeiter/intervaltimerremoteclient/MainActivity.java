package com.coachyeiter.intervaltimerremoteclient;

import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class MainActivity extends AppCompatActivity {

    EditText ipEdit, portEdit;
    TextView periodTimeText;
    Socket clientSocket;
    Lock clientCommunicationLock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ipEdit = findViewById(R.id.edit_ip);
        portEdit = findViewById(R.id.edit_port);
        periodTimeText = findViewById(R.id.text_period_time);
        clientSocket = null;
        clientCommunicationLock = new ReentrantLock();
    }

    public void connect(View view) {
        final String ipStr = ipEdit.getText().toString();
        final int port = Integer.parseInt(portEdit.getText().toString());
        new Thread(new Runnable() {
            @Override
            public void run() {
                clientCommunicationLock.lock();
                try {
                    clientSocket = new Socket(ipStr, port);
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    clientCommunicationLock.unlock();
                }
                startUpdateTimer();
            }
        }).start();
    }

    public void startUpdateTimer(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                    clientCommunicationLock.lock();
                    PrintStream ps = new PrintStream(clientSocket.getOutputStream());
                    BufferedReader br = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    ps.print("period_time");
                    ps.flush();

                    final String response = br.readLine();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            periodTimeText.setText(response);
                        }
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    clientCommunicationLock.unlock();
                }
                startUpdateTimer(); //Call again to repeat task
            }
        }).start();
    }

    public void pauseMedia(View view) {
        new SendMessageThread("pause_media").start();
    }

    public void pauseTimer(View view) {
        new SendMessageThread("pause_timer").start();
    }

    public void back30(View view) {
        new SendMessageThread("add_30").start();
    }

    public void back10(View view) {
        new SendMessageThread("add_10").start();
    }

    public void forward10(View view) {
        new SendMessageThread("remove_10").start();
    }

    public void forward30(View view) {
        new SendMessageThread("remove_30").start();
    }

    public void previousPeriod(View view) {
        new SendMessageThread("previous_period").start();
    }

    public void nextPeriod(View view) {
        new SendMessageThread("next_period").start();
    }

    class SendMessageThread extends Thread{
        String message;

        public SendMessageThread(String message){
            this.message = message;
        }
        @Override
        public void run() {
            clientCommunicationLock.lock();
            if (clientSocket != null && clientSocket.isConnected()){
                try {
                    PrintStream ps = new PrintStream(clientSocket.getOutputStream());
                    BufferedReader br = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    ps.print(message);
                    ps.flush();

                    String response = br.readLine();
                    Log.i("Response From Timer", response);

                    ps.print("period_time");
                    ps.flush();
                    final String timeResponse = br.readLine();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            periodTimeText.setText(timeResponse);
                        }
                    });

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            clientCommunicationLock.unlock();
        }
    }
}
