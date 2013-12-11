package umich.hijack.apps.ontimefreqsweep;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import umich.hijack.core.Packet;
import umich.hijack.core.PacketDispatch;
import umich.hijack.core.PacketType;
import umich.hijack.core.PktRecvCb;
import umich.hijack.core.SerialDecoder;
import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends Activity {

	private SerialDecoder _serialDecoder;
	private PacketDispatch _dispatcher;

	private boolean _nextFlag = false;
	private boolean powerActive = true;

	// Log file for saving
	private File logfile;
	private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
	private FileWriter fw;
	private BufferedWriter logfile_bw;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// Create a log file for this run of the application
		newLogFile();

		_dispatcher = new PacketDispatch();
		_serialDecoder = new SerialDecoder();

		_serialDecoder.registerPacketReceivedCallback(_dispatcher);
		_serialDecoder.registerPacketSentCallback(_dispatcher);
		_serialDecoder.setPowerFreq(12000);

		_dispatcher.registerPacketTransmitter(_serialDecoder);
		_dispatcher.registerIncomingPacketListener(bootedPkt, PacketType.BOOTED);
		_dispatcher.registerIncomingPacketListener(resumedPkt, PacketType.RESUMED);
		_dispatcher.registerIncomingPacketListener(powerdownPkt, PacketType.POWERDOWN);

		//_dispatcher.registerOutgoingByteListener(_outgoingByteListener);

		System.out.println("woah");

		final Button b1 = (Button)findViewById(R.id.button1);
		b1.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				EditText startFreqEditText = (EditText)findViewById(R.id.startFreq);
				EditText endFreqEditText = (EditText)findViewById(R.id.endFreq);
				EditText msStepEditText = (EditText)findViewById(R.id.stepMs);
				EditText hzStepEditText = (EditText)findViewById(R.id.stepHz);
				final TextView currFreqTextView = (TextView)findViewById(R.id.currFreq);

				final int startFreq = Integer.parseInt(startFreqEditText.getText().toString());
				final int endFreq = Integer.parseInt(endFreqEditText.getText().toString());
				final int msStep = Integer.parseInt(msStepEditText.getText().toString());
				final int hzStep = Integer.parseInt(hzStepEditText.getText().toString());

				b1.setText("Running...");



				Thread t = new Thread(new Runnable() {
					int currentFreq = startFreq;

					@Override
					public void run() {

						powerActive = true;

						while (currentFreq <= endFreq) {

							if (!powerActive) {
								runOnUiThread(new Runnable() {
									@Override
									public void run () {
										currFreqTextView.setText("Stopped");
									}
								});
								break;
							}

							_serialDecoder.setPowerFreq(currentFreq);
							currentFreq += hzStep;

							runOnUiThread(new Runnable() {
								@Override
								public void run () {
									currFreqTextView.setText(Integer.toString(currentFreq) + " Hz");
								}
							});

							// Add note in log that the frequency has changed
							addToLog("set frequency: " + Integer.toString(currentFreq) + " Hz\n");

							System.out.println("set frequency: " + Integer.toString(currentFreq) + " Hz");

							if (msStep == 0) {
								while (!_nextFlag) {
									try {
										Thread.sleep(100);
									} catch (InterruptedException e) {
										e.printStackTrace();
									}
								}

								_nextFlag = false;
							}
							else {
								try {
									Thread.sleep(msStep);
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
							}
						}

						try {
							logfile_bw.close();
						} catch (IOException e) {
							e.printStackTrace();
						}

						MainActivity.this.runOnUiThread(new Runnable() {
							@Override
							public void run() {
								b1.setText("Start");
							}
						});
					}
				});

				t.start();
			}
		});

		final Button b2 = (Button)findViewById(R.id.button2);
		b2.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				_nextFlag = true;
			}
		});

		final Button b3 = (Button)findViewById(R.id.button3);
		b3.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (powerActive) {
					powerActive = false;
					_serialDecoder.stop();
					MainActivity.this.runOnUiThread(new Runnable() {
						@Override
						public void run() {
							b3.setText("Start");
						}
					});
				} else {
					powerActive = true;
					_serialDecoder.start();
					MainActivity.this.runOnUiThread(new Runnable() {
						@Override
						public void run() {
							b3.setText("Stop");
						}
					});
				}
			}
		});
    }

	@Override
	public void onPause() {
		_serialDecoder.stop();
		super.onPause();
	}

	@Override
	public void onResume() {
		_serialDecoder.start();
		newLogFile();
		System.out.println("RESUME");
		super.onResume();
	}

	///////////////////////////////////////////////
	// Listeners
	///////////////////////////////////////////////
	/*private final OutgoingByteListener _outgoingByteListener = new OutgoingByteListener() {
		@Override
		public void OutgoingByteTransmit(int[] outgoingRaw) {
			synchronized (MainActivity.this) {
				//_pendingTransmitBytes += outgoingRaw.length;
			}

			for (int i = 0; i < outgoingRaw.length; i++) {
				_serialDecoder.sendByte(outgoingRaw[i]);
			}
		}
	};*/

	private final PktRecvCb bootedPkt = new PktRecvCb() {
		@Override
		public void recvPacket(Packet packet) {
			System.out.println("GOT BOOTED PACKET");
			addToLog("booted\n");
		}
	};

	private final PktRecvCb resumedPkt = new PktRecvCb() {
		@Override
		public void recvPacket(Packet packet) {
			addToLog("resumed\n");
		}
	};

	private final PktRecvCb powerdownPkt = new PktRecvCb() {
		@Override
		public void recvPacket(Packet packet) {
			addToLog("power down\n");
		}
	};

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	private void newLogFile () {
		// Create a log file for this run of the application
		String date_now_str = sdf.format(new Date());
		logfile = new File(getExternalFilesDir(null), "on_time_freq_sweep_log_" + date_now_str + ".txt");
		try {
			fw = new FileWriter(logfile.getAbsoluteFile());
		} catch (IOException e) {
			e.printStackTrace();
		}
		logfile_bw = new BufferedWriter(fw);
	}

	private void addToLog (String s) {
		// Add note in log
		String date_now_str = sdf.format(new Date());
		try {
			logfile_bw.write(date_now_str + ": " + s);
		} catch (IOException e) {
			//e.printStackTrace();
		}
	}
}
