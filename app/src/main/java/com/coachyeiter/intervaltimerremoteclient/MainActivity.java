package com.coachyeiter.intervaltimerremoteclient;

import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

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
    Button connectBtn;
    Socket clientSocket;
    Lock clientCommunicationLock;
    SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ipEdit = findViewById(R.id.edit_ip);
        portEdit = findViewById(R.id.edit_port);
        connectBtn = findViewById(R.id.btn_connect);
        periodTimeText = findViewById(R.id.text_period_time);
        clientSocket = null;
        clientCommunicationLock = new ReentrantLock();

        prefs = getApplicationContext().getSharedPreferences("prefs",MODE_PRIVATE);
        String lastIP = prefs.getString("ip", "");
        String lastPort = prefs.getString("port", "");
        ipEdit.setText(lastIP);
        portEdit.setText(lastPort);
    }

    public void connect(View view) {
        final String ipStr = ipEdit.getText().toString();
        final int port = Integer.parseInt(portEdit.getText().toString());

        SharedPreferences.Editor prefsEditor = prefs.edit();
        prefsEditor.putString("ip", ipStr);
        prefsEditor.putString("port", Integer.toString(port));
        prefsEditor.commit();

        new Thread(new Runnable() {
            @Override
            public void run() {
                clientCommunicationLock.lock();
                try {
                    clientSocket = new Socket(ipStr, port);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "Connected!", Toast.LENGTH_SHORT).show();
                            ipEdit.setEnabled(false);
                            portEdit.setEnabled(false);
                            connectBtn.setEnabled(false);

                        }
                    });

                } catch (IOException e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "Couldn\' Connect", Toast.LENGTH_SHORT).show();
                        }
                    });

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

    public void pauseMedia(View view) { new SendMessageThread("pause_media").start(); }

    public void volDown(View view) { new SendMessageThread("media_vol_down").start(); }

    public void volUp(View view) { new SendMessageThread("media_vol_up").start(); }

    public void skipSong(View view) {
        new SendMessageThread("skip_song").start();
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
			try
			{
				if (clientSocket != null && clientSocket.isConnected()){
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
				}
			}
			catch (IOException e) 
			{
				e.printStackTrace();
			}
			finally
			{
				clientCommunicationLock.unlock();
			}
        }
    }
}
