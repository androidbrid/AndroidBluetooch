package com.example.androidbluetooch;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import com.baoyz.swipemenulistview.SwipeMenu;
import com.baoyz.swipemenulistview.SwipeMenuCreator;
import com.baoyz.swipemenulistview.SwipeMenuItem;
import com.baoyz.swipemenulistview.SwipeMenuListView;
import com.baoyz.swipemenulistview.SwipeMenuListView.OnMenuItemClickListener;
import android.app.Activity;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class MainActivity extends Activity implements OnClickListener, OnItemClickListener {

	protected static final String TAG = "MainActivity";
	// 扫描、断开、发送蓝牙button
	private Button btn_scan_device, btn_disconnect, btn_send;
	// 未配对蓝牙的listview
	private ListView lv_disconnect, listview_msgxianshi;
	// 已配对蓝牙的listview,自定义的listview带滑动删除
	private SwipeMenuListView lv_connect;
	// 输入数据的edittext
	private EditText ed_inout;

	private int REQUEST_ENABLE_BT = 1;
	private List<String> list = new ArrayList<String>();
	private ConnectThread mConnectThread;
	public ConnectedThread mConnectedThread;

	private Dialog progressDialog;
	private TextView msg;
	// 蓝牙适配器
	private BluetoothAdapter mBluetoothAdapter;
	// 已经配对的蓝牙ArrayList
	private ArrayList<BluetoothDevice> data_connect = new ArrayList<BluetoothDevice>();
	// 未配对的蓝牙ArrayList
	private ArrayList<BluetoothDevice> data_disconnect = new ArrayList<BluetoothDevice>();
	// 自定义的adapater,已经配对的蓝牙,未配对的蓝牙
	private LeDeviceListAdapter connectListAdapter, disconnectListAdapter;
	// HC-08蓝牙连接状态
	private boolean mConnected = false;
	// HC-06蓝牙连接状态
	private boolean mhc06Connected = false;
	// 连接成功的蓝牙名字
	private String connect_string;
	// 未配对的HC-06,08点击的标记位
	private int data_onitemclick, datahc06_onitemclick;
	// HC-06:判断是已经配对的蓝牙还是未配对的蓝牙,断开连接的视图更新
	private boolean disconnect_flag = false;
	// HC-08蓝牙地址
	private String mDeviceAddress;
	// 蓝牙设备
	private BluetoothDevice dataconnectBean;
	// 蓝牙service,负责后台的蓝牙服务
	private static BluetoothLeService mBluetoothLeService;
	// 蓝牙的连接状态
	private String status = "disconnected";
	// 蓝牙特征值
	private static BluetoothGattCharacteristic target_chara = null;
	private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
	// 蓝牙4.0的UUID,其中0000ffe1-0000-1000-8000-00805f9b34fb是广州汇承信息科技有限公司08蓝牙模块的UUID
	public static String HEART_RATE_MEASUREMENT = "0000ffe1-0000-1000-8000-00805f9b34fb";
	// HC-06蓝牙UUID
	private static final String SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB";
	private List<Integer> mBuffer = new ArrayList<Integer>();

	private List<ChatMsgEntity> mDataArrays = new ArrayList<ChatMsgEntity>();
	private ChatMsgViewAdapter mAdapter;
	private SelfDialog selfDialog;
	private Handler mhandler = new Handler();
	private Handler myHandler = new Handler() {
		// 2.重写消息处理函数
		public void handleMessage(Message msg) {
			switch (msg.what) {
			// 判断发送的消息
			case 1: {
				// 更新View
				if (mConnected == true) {
					String state = msg.getData().getString("connect_state");
					setTitle(connect_string + ":" + state);
				} else {
					String state = msg.getData().getString("connect_state");
					setTitle(state);
				}

				break;
			}
			case 2: {
				// 更新View
				if (mhc06Connected == true) {
					String state = msg.getData().getString("connect_state");
					setTitle(connect_string + ":" + state);

					data_connect.add(data_disconnect.get(datahc06_onitemclick));

					data_disconnect.remove(datahc06_onitemclick);
					disconnectListAdapter = new LeDeviceListAdapter(MainActivity.this, data_disconnect);
					// 为listview指定适配器
					lv_disconnect.setAdapter(disconnectListAdapter);
					disconnectListAdapter.notifyDataSetChanged();

					connectListAdapter = new LeDeviceListAdapter(MainActivity.this, data_connect);
					// 为listview指定适配器
					lv_connect.setAdapter(connectListAdapter);
					connectListAdapter.notifyDataSetChanged();
				} else {
					String state = msg.getData().getString("connect_state");
					setTitle(state);
				}

				break;
			}
			case 3: {
				// 更新View
				if (mhc06Connected == true) {
					String state = msg.getData().getString("connect_state");
					setTitle(connect_string + ":" + state);
				} else {
					String state = msg.getData().getString("connect_state");
					setTitle(state);
				}

				break;
			}
			case 4: {
				String state = msg.getData().getString("updata_msg");
				listview_msg_stringReceiver(new String(state));
				break;
			}
			}
			super.handleMessage(msg);
		}

	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		btn_scan_device = (Button) findViewById(R.id.btn_scandevice);
		btn_disconnect = (Button) findViewById(R.id.btn_disconnectdevice);
		ed_inout = (EditText) findViewById(R.id.ed_inout);
		btn_send = (Button) findViewById(R.id.btn_send);
		lv_connect = (SwipeMenuListView) findViewById(R.id.lv_connect);
		lv_disconnect = (ListView) findViewById(R.id.lv_disconnect);
		listview_msgxianshi = (ListView) findViewById(R.id.listView1);
		btn_scan_device.setOnClickListener(this);
		btn_disconnect.setOnClickListener(this);
		btn_send.setOnClickListener(this);
		lv_disconnect.setOnItemClickListener(this);
		lv_connect.setOnItemClickListener(this);

		// mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		mBluetoothAdapter = bluetoothManager.getAdapter();
		// 打开蓝牙权限
		if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
			Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		}
		Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
		if (pairedDevices.size() > 0) {
			for (BluetoothDevice device : pairedDevices) {
				// DeviceBean bean = new DeviceBean();
				// bean.setAddress(device.getAddress());
				// bean.setName(device.getName());
				data_connect.add(device);
				// unpairDevice(device);
			}
		} else {
			Toast.makeText(MainActivity.this, "没有已配对的设备", Toast.LENGTH_SHORT).show();
		}

		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		this.registerReceiver(mReceiver, filter);

		// Register for broadcasts when discovery has finished
		filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		this.registerReceiver(mReceiver, filter);

		if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
			msgDialog("您的手机系统低于4.3,不支持ble4.0蓝牙");
		} else {
			msgDialog("您的手机系统高于4.3,支持ble4.0蓝牙");
		}
		connectListAdapter = new LeDeviceListAdapter(MainActivity.this, data_connect);
		// 为listview指定适配器
		lv_connect.setAdapter(connectListAdapter);
		disconnectListAdapter = new LeDeviceListAdapter(MainActivity.this, data_disconnect);
		// 为listview指定适配器
		lv_disconnect.setAdapter(disconnectListAdapter);

		/* 启动蓝牙service */
		Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
		bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

		// 绑定广播接收器
		registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());

		progressDialog = new Dialog(MainActivity.this, R.style.progress_dialog);
		progressDialog.setContentView(R.layout.dialog);
		progressDialog.setCancelable(true);
		progressDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
		msg = (TextView) progressDialog.findViewById(R.id.id_tv_loadingmsg);
		progressDialog.setCanceledOnTouchOutside(false);

		SwipeMenuCreator creator = new SwipeMenuCreator() {

			@Override
			public void create(SwipeMenu menu) {
				// create "open" item
				SwipeMenuItem openItem = new SwipeMenuItem(getApplicationContext());
				// set item background
				openItem.setBackground(new ColorDrawable(Color.rgb(0xC9, 0xC9, 0xCE)));
				// set item width
				openItem.setWidth(dp2px(90));
				// set item title
				openItem.setTitle("取消配对");
				// set item title fontsize
				openItem.setTitleSize(14);
				// set item title font color
				openItem.setTitleColor(Color.WHITE);
				// add to menu
				menu.addMenuItem(openItem);
			}
		};
		// set creator
		lv_connect.setMenuCreator(creator);

		// step 2. listener item click event
		lv_connect.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			@Override
			public void onMenuItemClick(int position, SwipeMenu menu, int index) {
				if (data_connect.get(position).getName().equals("HC-06")) {
					if_or_notPair();
					unpairDevice(data_connect.get(position));
					data_connect.remove(position);
					connectListAdapter.notifyDataSetChanged();
				} else {
					Toast.makeText(MainActivity.this, "HC-06支持取消配对,别的蓝牙设备无法消配对", Toast.LENGTH_SHORT).show();
				}
			}
		});

		mAdapter = new ChatMsgViewAdapter(getApplicationContext(), mDataArrays);
		listview_msgxianshi.setAdapter(mAdapter);
		listview_msgxianshi.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(
						getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
				return false;
			}
		});
	}

	// 取消已经配对的蓝牙(HC-06)
	private void unpairDevice(BluetoothDevice device) {
		try {
			Method m = device.getClass().getMethod("removeBond", (Class[]) null);
			m.invoke(device, (Object[]) null);
		} catch (Exception e) {
			Log.e(TAG, e.getMessage());
		}
	}

	private void msgDialog(String string) {
		selfDialog = new SelfDialog(MainActivity.this);
		selfDialog.setTitle("提示");
		selfDialog.setMessage(string);
		selfDialog.setYesOnclickListener("确定", new SelfDialog.onYesOnclickListener() {
			@Override
			public void onYesClick() {
				// Toast.makeText(MainActivity.this,"点击了--确定--按钮",Toast.LENGTH_LONG).show();
				selfDialog.dismiss();
			}
		});
		selfDialog.setNoOnclickListener("取消", new SelfDialog.onNoOnclickListener() {
			@Override
			public void onNoClick() {
				// Toast.makeText(MainActivity.this,"点击了--取消--按钮",Toast.LENGTH_LONG).show();
				selfDialog.dismiss();
			}
		});
		selfDialog.show();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_scandevice:
			btn_scan_device.setEnabled(false);
			msg.setText("卖力加载中");
			progressDialog.show();
			doDiscovery();
			break;
		case R.id.btn_disconnectdevice:
			if (mConnected == true) {
				mBluetoothLeService.disconnect();
				for (int i = 0; i < data_connect.size(); i++) {
					if (data_connect.get(i).equals(dataconnectBean)) {

						data_disconnect.add(data_connect.get(i));
						data_connect.remove(i);
					}
				}
				connectListAdapter = new LeDeviceListAdapter(MainActivity.this, data_connect);
				// 为listview指定适配器
				lv_connect.setAdapter(connectListAdapter);
				connectListAdapter.notifyDataSetChanged();

				disconnectListAdapter = new LeDeviceListAdapter(MainActivity.this, data_disconnect);
				// 为listview指定适配器
				lv_disconnect.setAdapter(disconnectListAdapter);
				disconnectListAdapter.notifyDataSetChanged();
			}

			if_or_notPair();
			break;
		case R.id.btn_send:

			if (mConnected == false) {
				
			} else {
				if (ed_inout.getText().toString().equals("")) {
					Log.d(TAG, "111111");

				} else {
					Log.d(TAG, "222222");
					target_chara.setValue(ed_inout.getText().toString());
					// 调用蓝牙服务的写特征值方法实现发送数据
					mBluetoothLeService.writeCharacteristic(target_chara);
					listview_msg_stringSend();
				}
			}
			if (mhc06Connected == true) {
				if (ed_inout != null && !"".equals(ed_inout)) {
					try {
						mConnectedThread.write(ed_inout.getText().toString().getBytes());
						listview_msg_stringSend();
					} catch (Exception e) {
					}
				}

			}
			if(mConnected==false||mhc06Connected==false){
				listview_msg_stringSend();
			}
			ed_inout.setText("");
			break;
		default:
			break;
		}
	}

	private String getDate() {
		Calendar c = Calendar.getInstance();

		String year = String.valueOf(c.get(Calendar.YEAR));
		String month = String.valueOf(c.get(Calendar.MONTH)+ 1);
		String day = String.valueOf(c.get(Calendar.DAY_OF_MONTH));
		String hour = String.valueOf(c.get(Calendar.HOUR_OF_DAY));
		String mins = String.valueOf(c.get(Calendar.MINUTE));

		StringBuffer sbBuffer = new StringBuffer();
		sbBuffer.append(year + "-" + month + "-" + day + " " + hour + ":" + mins);

		return sbBuffer.toString();
	}

	private void listview_msg_stringSend() {
		/**
		 * 发送消息跟新View
		 */
		ChatMsgEntity entity = new ChatMsgEntity();
		entity.setDate(getDate());
		entity.setName("user");
		entity.setMsgType(false);
		entity.setText(ed_inout.getText().toString());
		mDataArrays.add(entity);
		mAdapter.notifyDataSetChanged();
		ed_inout.setText("");
		listview_msgxianshi.setSelection(listview_msgxianshi.getCount() - 1);
	}

	private void listview_msg_stringReceiver(String string) {
		/**
		 * 接收消息跟新View
		 */
		ChatMsgEntity entity = new ChatMsgEntity();
		entity.setDate(getDate());
		entity.setName("设备");
		entity.setMsgType(true);
		entity.setText(string);
		mDataArrays.add(entity);
		mAdapter.notifyDataSetChanged();
		listview_msgxianshi.setSelection(listview_msgxianshi.getCount() - 1);
	}

	private void if_or_notPair() {
		if (disconnect_flag == true) {
			if (mhc06Connected == true) {
				if (mConnectThread != null) {
					mConnectThread.cancel();
					mConnectThread = null;
				}
				if (mConnectedThread != null) {
					mConnectedThread.cancel();
					mConnectedThread = null;
				}
				mhc06Connected = false;
				status = "disconnected";
				// 更新连接状态
				Message msg1 = new Message();
				msg1.what = 3;
				Bundle b = new Bundle();
				b.putString("connect_state", status);
				msg1.setData(b);
				// 将连接状态更新的UI的textview上
				myHandler.sendMessage(msg1);

			}
		} else {
			if (mhc06Connected == true) {
				if (mConnectThread != null) {
					mConnectThread.cancel();
					mConnectThread = null;
				}
				if (mConnectedThread != null) {
					mConnectedThread.cancel();
					mConnectedThread = null;
				}
				mhc06Connected = false;
				status = "disconnected";
				// 更新连接状态
				Message msg2 = new Message();
				msg2.what = 2;
				Bundle b = new Bundle();
				b.putString("connect_state", status);
				msg2.setData(b);
				// 将连接状态更新的UI的textview上
				myHandler.sendMessage(msg2);

			}
		}
	}

	// 搜索蓝牙
	private void doDiscovery() {
		setTitle(R.string.scanning);
		if (mBluetoothAdapter.isDiscovering()) {
			mBluetoothAdapter.cancelDiscovery();
		}
		mBluetoothAdapter.startDiscovery();
	}

	// 搜索蓝牙的广播
	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();

			// When discovery finds a device
			if (BluetoothDevice.ACTION_FOUND.equals(action)) {
				// Get the BluetoothDevice object from the Intent
				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				// If it's already paired, skip it, because it's been listed
				// already
				if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
					// mNewDevicesArrayAdapter.add(device.getName() + "\n" +
					// device.getAddress());

					if (list.indexOf(device.getAddress().toString()) == -1) {
						Map<String, Object> listem = new HashMap<String, Object>();

						listem.put("dName", device.getName());
						listem.put("dAddress", device.getAddress().toString());
						Log.d("searchBtDevices==", list.toString());
						list.add(device.getAddress());
						Log.d("searchBtDevices==", list.toString());
						Log.d("Devices==", device.getAddress());
						Log.d("==", "not");

						// DeviceBean bean = new DeviceBean();
						// bean.setAddress(device.getAddress());
						// bean.setName(device.getName());

						data_disconnect.add(device);

						disconnectListAdapter = new LeDeviceListAdapter(MainActivity.this, data_disconnect);
						// 为listview指定适配器
						lv_disconnect.setAdapter(disconnectListAdapter);
						disconnectListAdapter.notifyDataSetChanged();
					} else {
						Log.d("==", "have");
					}
				}
			} else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
				btn_scan_device.setEnabled(true);
				progressDialog.dismiss();
				setTitle(R.string.select_device);
			}
		}
	};

	/* BluetoothLeService绑定的回调函数 */
	private final ServiceConnection mServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName componentName, IBinder service) {
			mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
			if (!mBluetoothLeService.initialize()) {
				Log.e("MainActivity.this", "Unable to initialize Bluetooth");
				finish();
			}
			// Automatically connects to the device upon successful start-up
			// initialization.
			// 根据蓝牙地址，连接设备
			// 每次连接之前关闭上一次连接，这样在第二次连接蓝牙的时候速度快
			// 博客参考http://bbs.eeworld.com.cn/thread-438571-1-1.html
			mBluetoothLeService.close();
			mBluetoothLeService.connect(mDeviceAddress);

		}

		@Override
		public void onServiceDisconnected(ComponentName componentName) {
			mBluetoothLeService = null;
		}

	};
	/**
	 * 广播接收器，负责接收BluetoothLeService类发送的数据
	 */
	private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();
			if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action))// Gatt连接成功
			{
				progressDialog.dismiss();
				mConnected = true;
				status = "connected";
				// 更新连接状态
				updateConnectionState(status);
				System.out.println("BroadcastReceiver :" + "device connected");

				data_connect.add(data_disconnect.get(data_onitemclick));

				data_disconnect.remove(data_onitemclick);
				disconnectListAdapter = new LeDeviceListAdapter(MainActivity.this, data_disconnect);
				// 为listview指定适配器
				lv_disconnect.setAdapter(disconnectListAdapter);
				disconnectListAdapter.notifyDataSetChanged();

				connectListAdapter = new LeDeviceListAdapter(MainActivity.this, data_connect);
				// 为listview指定适配器
				lv_connect.setAdapter(connectListAdapter);
				connectListAdapter.notifyDataSetChanged();

			} else if (BluetoothLeService.ACTION_GATT_DISCONNECTED// Gatt连接失败
					.equals(action)) {
				progressDialog.dismiss();
				mConnected = false;
				status = "disconnected";
				// 更新连接状态
				updateConnectionState(status);
				System.out.println("BroadcastReceiver :" + "device disconnected");

				for (int i = 0; i < data_connect.size(); i++) {
					if (data_connect.get(i).equals(dataconnectBean)) {

						data_disconnect.add(data_connect.get(i));
						data_connect.remove(i);
					}
				}
				connectListAdapter = new LeDeviceListAdapter(MainActivity.this, data_connect);
				// 为listview指定适配器
				lv_connect.setAdapter(connectListAdapter);
				connectListAdapter.notifyDataSetChanged();

				disconnectListAdapter = new LeDeviceListAdapter(MainActivity.this, data_disconnect);
				// 为listview指定适配器
				lv_disconnect.setAdapter(disconnectListAdapter);
				disconnectListAdapter.notifyDataSetChanged();

			} else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED// 发现GATT服务器
					.equals(action)) {
				// Show all the supported services and characteristics on the
				// user interface.
				// 获取设备的所有蓝牙服务
				displayGattServices(mBluetoothLeService.getSupportedGattServices());
				System.out.println("BroadcastReceiver :" + "device SERVICES_DISCOVERED");
			} else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {// 有效数据
				// 处理发送过来的数据
				Log.d(TAG, intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
				listview_msg_stringReceiver(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
			}
		}
	};

	/* 更新连接状态 */
	private void updateConnectionState(String status) {
		Message msg = new Message();
		msg.what = 1;
		Bundle b = new Bundle();
		b.putString("connect_state", status);
		msg.setData(b);
		// 将连接状态更新的UI的textview上
		myHandler.sendMessage(msg);
		System.out.println("connect_state:" + status);

	}

	/**
	 * @Title: displayGattServices @Description: TODO(处理蓝牙服务) @param 无 @return
	 *         void @throws
	 */
	private void displayGattServices(List<BluetoothGattService> gattServices) {

		if (gattServices == null)
			return;
		String uuid = null;
		// 服务数据,可扩展下拉列表的第一级数据
		ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();

		// 特征数据（隶属于某一级服务下面的特征值集合）
		ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData = new ArrayList<ArrayList<HashMap<String, String>>>();

		// 部分层次，所有特征值集合
		mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

		// Loops through available GATT Services.
		for (BluetoothGattService gattService : gattServices) {

			// 获取服务列表
			HashMap<String, String> currentServiceData = new HashMap<String, String>();
			uuid = gattService.getUuid().toString();

			// 查表，根据该uuid获取对应的服务名称。SampleGattAttributes这个表需要自定义。

			gattServiceData.add(currentServiceData);

			System.out.println("Service uuid:" + uuid);

			ArrayList<HashMap<String, String>> gattCharacteristicGroupData = new ArrayList<HashMap<String, String>>();

			// 从当前循环所指向的服务中读取特征值列表
			List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();

			ArrayList<BluetoothGattCharacteristic> charas = new ArrayList<BluetoothGattCharacteristic>();

			// Loops through available Characteristics.
			// 对于当前循环所指向的服务中的每一个特征值
			for (final BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
				charas.add(gattCharacteristic);
				HashMap<String, String> currentCharaData = new HashMap<String, String>();
				uuid = gattCharacteristic.getUuid().toString();

				if (gattCharacteristic.getUuid().toString().equals(HEART_RATE_MEASUREMENT)) {
					// 测试读取当前Characteristic数据，会触发mOnDataAvailable.onCharacteristicRead()
					mhandler.postDelayed(new Runnable() {

						@Override
						public void run() {
							mBluetoothLeService.readCharacteristic(gattCharacteristic);
						}
					}, 200);

					// 接受Characteristic被写的通知,收到蓝牙模块的数据后会触发mOnDataAvailable.onCharacteristicWrite()
					mBluetoothLeService.setCharacteristicNotification(gattCharacteristic, true);
					target_chara = gattCharacteristic;
					// 设置数据内容
					// 往蓝牙模块写入数据
					// mBluetoothLeService.writeCharacteristic(gattCharacteristic);
				}
				List<BluetoothGattDescriptor> descriptors = gattCharacteristic.getDescriptors();
				for (BluetoothGattDescriptor descriptor : descriptors) {
					System.out.println("---descriptor UUID:" + descriptor.getUuid());
					// 获取特征值的描述
					mBluetoothLeService.getCharacteristicDescriptor(descriptor);
					// mBluetoothLeService.setCharacteristicNotification(gattCharacteristic,
					// true);
				}

				gattCharacteristicGroupData.add(currentCharaData);
			}
			// 按先后顺序，分层次放入特征值集合中，只有特征值
			mGattCharacteristics.add(charas);
			// 构件第二级扩展列表（服务下面的特征值）
			gattCharacteristicData.add(gattCharacteristicGroupData);

		}

	}

	/* 意图过滤器 */
	private static IntentFilter makeGattUpdateIntentFilter() {
		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
		intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
		return intentFilter;
	}

	// HC-06蓝牙的连接方法
	public void connect(BluetoothDevice device) {
		Log.d(TAG, "connect to: " + device);
		// Start the thread to connect with the given device
		mConnectThread = new ConnectThread(device);
		mConnectThread.start();
	}

	/**
	 * This thread runs while attempting to make an outgoing connection with a
	 * device. It runs straight through; the connection either succeeds or
	 * fails.
	 */
	// HC-06蓝牙的连接方法
	private class ConnectThread extends Thread {
		private final BluetoothSocket mmSocket;

		public ConnectThread(BluetoothDevice device) {
			BluetoothSocket tmp = null;

			// Get a BluetoothSocket for a connection with the
			// given BluetoothDevice
			try {
				tmp = device.createRfcommSocketToServiceRecord(UUID.fromString(SPP_UUID));
			} catch (IOException e) {
				Log.e(TAG, "create() failed", e);
			}
			mmSocket = tmp;
		}

		public void run() {
			Log.i(TAG, "BEGIN mConnectThread");
			setName("ConnectThread");

			// Always cancel discovery because it will slow down a connection
			mBluetoothAdapter.cancelDiscovery();

			// Make a connection to the BluetoothSocket
			try {
				// This is a blocking call and will only return on a
				// successful connection or an exception
				mmSocket.connect();
			} catch (IOException e) {
				progressDialog.dismiss();
				Log.e(TAG, "unable to connect() socket", e);
				// Close the socket
				try {
					mmSocket.close();
				} catch (IOException e2) {
					Log.e(TAG, "unable to close() socket during connection failure", e2);
				}
				return;
			}

			mConnectThread = null;
			progressDialog.dismiss();
			if (disconnect_flag == true) {
				mhc06Connected = true;
				status = "connected";
				// 更新连接状态
				Message msg = new Message();
				msg.what = 3;
				Bundle b = new Bundle();
				b.putString("connect_state", status);
				msg.setData(b);
				// 将连接状态更新的UI的textview上
				myHandler.sendMessage(msg);
			} else {
				mhc06Connected = true;
				status = "connected";
				// 更新连接状态
				Message msg = new Message();
				msg.what = 2;
				Bundle b = new Bundle();
				b.putString("connect_state", status);
				msg.setData(b);
				// 将连接状态更新的UI的textview上
				myHandler.sendMessage(msg);
			}
			// Start the connected thread
			// Start the thread to manage the connection and perform
			// transmissions
			mConnectedThread = new ConnectedThread(mmSocket);
			mConnectedThread.start();

		}

		public void cancel() {
			try {
				mmSocket.close();
			} catch (IOException e) {
				Log.e(TAG, "close() of connect socket failed", e);
			}
		}
	}

	/**
	 * This thread runs during a connection with a remote device. It handles all
	 * incoming and outgoing transmissions.
	 */
	// HC-06蓝牙的连接方法
	private class ConnectedThread extends Thread {
		private final BluetoothSocket mmSocket;
		private final InputStream mmInStream;
		private final OutputStream mmOutStream;

		public ConnectedThread(BluetoothSocket socket) {
			Log.d(TAG, "create ConnectedThread");
			mmSocket = socket;
			InputStream tmpIn = null;
			OutputStream tmpOut = null;

			// Get the BluetoothSocket input and output streams
			try {
				tmpIn = socket.getInputStream();
				tmpOut = socket.getOutputStream();
			} catch (IOException e) {
				Log.e(TAG, "temp sockets not created", e);
			}

			mmInStream = tmpIn;
			mmOutStream = tmpOut;
		}

		public void run() {
			Log.i(TAG, "BEGIN mConnectedThread");
			byte[] buffer = new byte[256];
			int bytes;

			// Keep listening to the InputStream while connected
			while (true) {
				try {
					// //HC-06蓝牙的读数据方法
					bytes = mmInStream.read(buffer);
					Log.d(TAG, "" + new String(buffer));

					Message msg1 = new Message();
					msg1.what = 4;
					Bundle b = new Bundle();
					b.putString("updata_msg", new String(buffer));
					msg1.setData(b);
					// 将连接状态更新的UI的textview上
					myHandler.sendMessage(msg1);

					synchronized (mBuffer) {
						for (int i = 0; i < bytes; i++) {
							mBuffer.add(buffer[i] & 0xFF);
						}
					}
					// mHandler.sendEmptyMessage(MSG_NEW_DATA);
				} catch (IOException e) {
					Log.e(TAG, "disconnected", e);
					break;
				}
			}
		}

		/**
		 * Write to the connected OutStream.
		 * 
		 * @param buffer
		 *            The bytes to write
		 */
		// HC-06蓝牙的写数据方法
		public void write(byte[] buffer) {
			try {
				mmOutStream.write(buffer);
			} catch (IOException e) {
				Log.e(TAG, "Exception during write", e);
			}
		}

		public void cancel() {
			try {
				mmSocket.close();
			} catch (IOException e) {
				Log.e(TAG, "close() of connect socket failed", e);
			}
		}
	}

	private int dp2px(int dp) {
		return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
		switch (arg0.getId()) {
		// 已经配对的蓝牙点击事件
		case R.id.lv_connect:
			if (mConnected == true || mhc06Connected == true) {
				Toast.makeText(MainActivity.this, "请先断开当前连接", Toast.LENGTH_SHORT).show();
			} else {
				if (data_connect.get(position).getName().equals("HC-06")) {
					msg.setText("连接蓝牙中");
					progressDialog.show();
					BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(data_connect.get(position).getAddress());
					// Attempt to connect to the device
					connect(device);
					connect_string = data_connect.get(position).getName();
					datahc06_onitemclick = position;
					disconnect_flag = true;
				} else {
					Toast.makeText(getApplicationContext(), "该功能只支持连接HC-06,08蓝牙模块", Toast.LENGTH_SHORT).show();
				}
			}
			break;
		// 未配对的蓝牙点击事件
		case R.id.lv_disconnect:
			if (mConnected == true || mhc06Connected == true) {
				Toast.makeText(MainActivity.this, "请先断开当前连接", Toast.LENGTH_SHORT).show();
			} else {
				if (data_disconnect.get(position).getName().equals("HC-08")) {
					msg.setText("连接蓝牙中");
					progressDialog.show();
					data_onitemclick = position;
					dataconnectBean = data_disconnect.get(position);
					mDeviceAddress = data_disconnect.get(position).getAddress();
					Log.d(TAG, "111" + data_disconnect.get(position).getAddress());
					if (mBluetoothLeService != null) {
						// 每次连接之前关闭上一次连接，这样在第二次连接蓝牙的时候速度快
						// 博客参考http://bbs.eeworld.com.cn/thread-438571-1-1.html
						mBluetoothLeService.close();
						final boolean result = mBluetoothLeService.connect(mDeviceAddress);
						connect_string = data_disconnect.get(position).getName();
						Log.d(TAG, "Connect request result=" + result);
					}

				} else if (data_disconnect.get(position).getName().equals("HC-06")) {
					msg.setText("连接蓝牙中");
					progressDialog.show();
					BluetoothDevice device = mBluetoothAdapter
							.getRemoteDevice(data_disconnect.get(position).getAddress());
					// Attempt to connect to the device
					connect(device);
					connect_string = data_disconnect.get(position).getName();
					datahc06_onitemclick = position;
				} else {
					Toast.makeText(getApplicationContext(), "该功能只支持连接HC-06,08蓝牙模块", Toast.LENGTH_SHORT).show();
				}
			}
			break;
		default:
			break;
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		// Make sure we're not doing discovery anymore
		if (mBluetoothAdapter != null) {
			mBluetoothAdapter.cancelDiscovery();
		}

		// Unregister broadcast listeners
		this.unregisterReceiver(mReceiver);

		// 解除广播接收器
		unregisterReceiver(mGattUpdateReceiver);
		mBluetoothLeService = null;
	}
}
