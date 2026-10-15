package com.daitj.easycontrolfork.app.helper;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.util.Pair;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Toast;

import java.io.InputStream;
import java.util.ArrayList;

import com.daitj.easycontrolfork.app.DeviceDetailActivity;
import com.daitj.easycontrolfork.app.R;
import com.daitj.easycontrolfork.app.client.Client;
import com.daitj.easycontrolfork.app.client.tools.AdbTools;
import com.daitj.easycontrolfork.app.databinding.ItemDevicesItemBinding;
import com.daitj.easycontrolfork.app.databinding.ItemLoadingBinding;
import com.daitj.easycontrolfork.app.databinding.ItemSetDeviceBinding;
import com.daitj.easycontrolfork.app.entity.AppData;
import com.daitj.easycontrolfork.app.entity.Device;

public class DeviceListAdapter extends BaseAdapter {

  private final Context context;
  private Device sendFileDevice;
  public DeviceListAdapter(Context c) {
    queryDevices();
    context = c;
  }

  @Override
  public int getCount() {
    return AdbTools.devicesList.size();
  }

  @Override
  public Object getItem(int i) {
    return null;
  }

  @Override
  public long getItemId(int i) {
    return 0;
  }

  @Override
  public View getView(int i, View view, ViewGroup viewGroup) {
    if (view == null) {
      ItemDevicesItemBinding devicesItemBinding = ItemDevicesItemBinding.inflate(LayoutInflater.from(context));
      view = devicesItemBinding.getRoot();
      view.setTag(devicesItemBinding);
    }
    // 获取设备
    Device device = AdbTools.devicesList.get(i);
    ItemDevicesItemBinding devicesItemBinding = (ItemDevicesItemBinding) view.getTag();
    // 设置卡片值
    boolean grid = AppData.setting.getHomeGrid();
    int containerColor = resolveThemeColor(com.google.android.material.R.attr.colorPrimaryContainer);
    int onContainerColor = resolveThemeColor(com.google.android.material.R.attr.colorOnPrimaryContainer);
    devicesItemBinding.deviceIconBackground.setBackgroundTintList(ColorStateList.valueOf(containerColor));
    devicesItemBinding.deviceIcon.setImageTintList(ColorStateList.valueOf(onContainerColor));
    devicesItemBinding.getRoot().setOrientation(grid ? android.widget.LinearLayout.VERTICAL : android.widget.LinearLayout.HORIZONTAL);
    android.widget.LinearLayout textLayout = (android.widget.LinearLayout) devicesItemBinding.deviceType.getParent();
    android.widget.LinearLayout.LayoutParams textLayoutParams = (android.widget.LinearLayout.LayoutParams) textLayout.getLayoutParams();
    if (grid) {
      devicesItemBinding.getRoot().setGravity(android.view.Gravity.CENTER);
      textLayoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
      textLayoutParams.weight = 0;
      devicesItemBinding.deviceType.setGravity(android.view.Gravity.CENTER);
      devicesItemBinding.deviceName.setGravity(android.view.Gravity.CENTER);
      textLayout.setPadding(0, textLayout.getPaddingTop(), 0, textLayout.getPaddingBottom());
    } else {
      devicesItemBinding.getRoot().setGravity(android.view.Gravity.CENTER_VERTICAL);
      textLayoutParams.width = 0;
      textLayoutParams.weight = 1;
      int textPadding = Math.round(12 * context.getResources().getDisplayMetrics().density);
      textLayout.setPadding(textPadding, textLayout.getPaddingTop(), 0, textLayout.getPaddingBottom());
      devicesItemBinding.deviceType.setGravity(android.view.Gravity.START);
      devicesItemBinding.deviceName.setGravity(android.view.Gravity.START);
    }
    textLayout.setLayoutParams(textLayoutParams);
    devicesItemBinding.deviceIcon.setImageResource(device.isNetworkDevice() ? R.drawable.wifi : R.drawable.link);
    devicesItemBinding.deviceType.setText(context.getString(device.isNetworkDevice() ? R.string.main_device_type_network : R.string.main_device_type_link));
    devicesItemBinding.deviceName.setText(device.name);
    // 单击事件
    devicesItemBinding.getRoot().setOnClickListener(v -> Client.startDevice(device));
    // 长按事件
    devicesItemBinding.getRoot().setOnLongClickListener(v -> {
      onLongClickCard(device);
      return true;
    });
    return view;
  }

  private int resolveThemeColor(int attribute) {
    TypedValue value = new TypedValue();
    context.getTheme().resolveAttribute(attribute, value, true);
    return value.data;
  }

  // 卡片长按事件
  private void onLongClickCard(Device device) {
    ItemSetDeviceBinding itemSetDeviceBinding = ItemSetDeviceBinding.inflate(LayoutInflater.from(context));
    Dialog dialog = ViewTools.createDialog(context, true, itemSetDeviceBinding.getRoot());
    // 有线设备
    itemSetDeviceBinding.buttonStartWireless.setVisibility(device.isLinkDevice() ? View.VISIBLE : View.GONE);
    // 设置监听
    itemSetDeviceBinding.buttonStartWireless.setOnClickListener(v -> {
      dialog.cancel();
      restartWireless(device);
    });
    itemSetDeviceBinding.buttonRecover.setOnClickListener(v -> {
      dialog.cancel();
      reset(device);
    });
    itemSetDeviceBinding.buttonChange.setOnClickListener(v -> {
      dialog.cancel();
      Intent intent = new Intent(context, DeviceDetailActivity.class);
      intent.putExtra("uuid", device.uuid);
      context.startActivity(intent);
    });
    itemSetDeviceBinding.buttonDelete.setOnClickListener(v -> {
      AppData.dbHelper.delete(device);
      update();
      dialog.cancel();
    });
    dialog.show();
    itemSetDeviceBinding.buttonPushFile.setOnClickListener(v -> {
      dialog.cancel();
      sendFileDevice = device;
      Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
      intent.setType("*/*");
      intent.addCategory(Intent.CATEGORY_OPENABLE);
      intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);
      ((Activity) context).startActivityForResult(intent, 1);
    });
  }

  private void queryDevices() {
    ArrayList<Device> rawDevices = AppData.dbHelper.getAll();
    ArrayList<Device> tmp1 = new ArrayList<>();
    ArrayList<Device> tmp2 = new ArrayList<>();
    for (Device device : rawDevices) {
      if (device.isLinkDevice() && AdbTools.usbDevicesList.containsKey(device.address)) tmp1.add(device);
      else if (device.isNetworkDevice()) tmp2.add(device);
    }
    AdbTools.devicesList.clear();
    AdbTools.devicesList.addAll(tmp1);
    AdbTools.devicesList.addAll(tmp2);
  }

  private void restartWireless(Device device) {
    Pair<ItemLoadingBinding, Dialog> loading = ViewTools.createLoading(context);
    loading.second.show();
    AdbTools.restartOnTcpip(device, result -> AppData.uiHandler.post(() -> {
      loading.second.cancel();
      Toast.makeText(context, context.getString(result ? R.string.toast_success : R.string.toast_fail), Toast.LENGTH_SHORT).show();
    }));
  }

  private void reset(Device device) {
    Pair<ItemLoadingBinding, Dialog> loading = ViewTools.createLoading(context);
    loading.second.show();
    AdbTools.runOnceCmd(device, "wm size reset", result -> AppData.uiHandler.post(() -> {
      loading.second.cancel();
      Toast.makeText(context, context.getString(result ? R.string.toast_success : R.string.toast_fail), Toast.LENGTH_SHORT).show();
    }));
  }

  public void pushFile(InputStream inputStream, String fileName) {
    if (inputStream == null) return;
    Pair<ItemLoadingBinding, Dialog> loading = ViewTools.createLoading(context);
    loading.second.show();
    AdbTools.pushFile(sendFileDevice, inputStream, fileName, process -> AppData.uiHandler.post(() -> {
      if (process < 0) {
        loading.second.cancel();
        Toast.makeText(context, context.getString(R.string.toast_fail), Toast.LENGTH_SHORT).show();
      } else if (process == 100) {
        loading.second.cancel();
        Toast.makeText(context, context.getString(R.string.toast_success), Toast.LENGTH_SHORT).show();
      } else loading.first.text.setText(process + " %");
    }));
  }

  public void update() {
    queryDevices();
    notifyDataSetChanged();
  }

}
