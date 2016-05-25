package com.example.mobilesafe.activity;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.json.JSONException;
import org.json.JSONObject;

import com.example.mobilesafe.R;
import com.example.mobilesafe.utils.StreamUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class SplashActivity extends Activity {
	protected static final String TAG = "SplashActivity";

	protected static final int CODE_UPDATE_DIALOG = 0;
	protected static final int CODE_URL_ERROR = 1;
	protected static final int CODE_NET_ERROR = 2;
	protected static final int CODE_JSON_ERROR = 3;
	protected static final int CODE_ENTER_HOME = 4; // 进入主页面

	private TextView tvVersion;
	private TextView tvProgress; // 下载进度显示

	// 服务器返回的信息
	private String serverVersionName; // 版本名
	private int serverVersionCode; // 版本号
	private String serverDescription; // 版本描述
	private String serverDownloadUrl; // 下载地址

	private Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {

			switch (msg.what) {
			case CODE_UPDATE_DIALOG:
				showDialog();
				break;
			case CODE_URL_ERROR:
				Toast.makeText(SplashActivity.this, "URL错误", Toast.LENGTH_SHORT).show();
				enterHome();
				break;
			case CODE_NET_ERROR:
				Toast.makeText(SplashActivity.this, "NET错误", Toast.LENGTH_SHORT).show();
				enterHome();
				break;
			case CODE_JSON_ERROR:
				Toast.makeText(SplashActivity.this, "JSON数据解析错误", Toast.LENGTH_SHORT).show();
				enterHome();
				break;
			case CODE_ENTER_HOME:
				enterHome();
				break;
			default:
				break;
			}

			super.handleMessage(msg);
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_splash);

		tvVersion = (TextView) this.findViewById(R.id.tv_version);
		tvVersion.setText("版本号是:" + getVersionName());
		// tvProgress = (TextView) findViewById(R.id.tv_progress);// 默认隐藏
		checkVersion();
	}

	/**
	 * 获取app的版本名称
	 */
	private String getVersionName() {
		PackageManager pm = getPackageManager();
		try {
			PackageInfo info = pm.getPackageInfo(getPackageName(), 0);
			return info.versionName;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
			return "";
		}
	}

	/**
	 * 获取app的版本号
	 */
	private int getVersionCode() {
		PackageManager pm = getPackageManager();
		try {
			PackageInfo info = pm.getPackageInfo(getPackageName(), 0);
			int versionCode = info.versionCode;
			return versionCode;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}

		return -1;
	}

	/**
	 * 从服务端获取版本信息进行校验
	 */
	private void checkVersion() {
		final long startTime = System.currentTimeMillis();
		// 启动子线程异步加载数据
		new Thread() {
			public void run() {
				Message msg = Message.obtain();
				HttpURLConnection conn = null;
				try {
					URL url = new URL(getString(R.string.serverurl));
					conn = (HttpURLConnection) url.openConnection();
					conn.setRequestMethod("GET"); // 设置请求方法
					conn.setConnectTimeout(5000); // 设置连接超时
					conn.setReadTimeout(5000); // 设置响应超时，连接上但服务器无响应时间
					conn.connect(); // 连接服务器

					int responseCode = conn.getResponseCode();
					if (responseCode == 200) {
						InputStream inputStream = conn.getInputStream();
						String result = StreamUtils.readFromStream(inputStream);
						// System.out.println("网络返回:" + result);
						Log.i(TAG, "网络返回" + result);

						JSONObject json = new JSONObject(result);
						serverVersionName = json.getString("versionName");
						serverVersionCode = json.getInt("versionCode");
						serverDescription = json.getString("description");
						serverDownloadUrl = json.getString("downloadUrl");

						// Log.i(TAG, "版本描述" + serverDescription);

						if (serverVersionCode > getVersionCode()) {
							// 説明有更新，彈出對話框说明
							// Log.i(TAG, "有更新，最新的版本号是" + serverVersionCode);
							msg.what = CODE_UPDATE_DIALOG;
						} else {
							// 没有更新
							msg.what = CODE_ENTER_HOME;
						}
					}

				} catch (MalformedURLException e) {
					// URL 错误的异常
					msg.what = CODE_URL_ERROR;
					e.printStackTrace();
				} catch (IOException e) {
					// 网络异常
					msg.what = CODE_NET_ERROR;
					e.printStackTrace();
				} catch (JSONException e) {
					// JSON解析失败
					msg.what = CODE_JSON_ERROR;
					e.printStackTrace();
				} finally {
					long endTime = System.currentTimeMillis();
					long timeUsed = endTime - startTime;// 访问网络房费的时间

					if (timeUsed < 2000) {
						// 强制休眠一段时间,保证闪屏页展示2秒钟
						try {
							Thread.sleep(2000 - timeUsed);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}

					// 把消息传给消息队列
					mHandler.sendMessage(msg);

					if (conn != null) {
						conn.disconnect();// 关闭网络连接
					}
				}

			}
		}.start();
	}

	/**
	 * 升级对话框
	 */
	protected void showDialog() {
		// TODO Auto-generated method stub
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("最新版本" + serverVersionName);
		builder.setMessage(serverDescription);

		// builder.setCancelable(false);//不让用户取消对话框, 用户体验太差,尽量不要用

		builder.setPositiveButton("立即更新", new OnClickListener() {

			public void onClick(DialogInterface dialog, int which) {
				Log.i(TAG, "立即更新");
				// download();
			}
		});

		builder.setNegativeButton("下次再说", new OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				Log.i(TAG, "下次再说");
				enterHome();
			}
		});

		builder.setOnCancelListener(new OnCancelListener() {

			@Override
			public void onCancel(DialogInterface dialog) {
				Log.i(TAG, "点击取消");
			}
		});

		builder.show();
	}

	/**
	 * 直接进入主页面
	 */
	protected void enterHome() {
		Intent intent = new Intent(this, HomeActivity.class);
		startActivity(intent);
		finish();

	}
}
