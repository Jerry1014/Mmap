package com.example.mymap;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapPoi;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.overlayutil.BikingRouteOverlay;
import com.baidu.mapapi.overlayutil.DrivingRouteOverlay;
import com.baidu.mapapi.overlayutil.IndoorRouteOverlay;
import com.baidu.mapapi.overlayutil.MassTransitRouteOverlay;
import com.baidu.mapapi.overlayutil.TransitRouteOverlay;
import com.baidu.mapapi.overlayutil.WalkingRouteOverlay;
import com.baidu.mapapi.search.core.PoiInfo;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.poi.OnGetPoiSearchResultListener;
import com.baidu.mapapi.search.poi.PoiDetailResult;
import com.baidu.mapapi.search.poi.PoiDetailSearchResult;
import com.baidu.mapapi.search.poi.PoiIndoorResult;
import com.baidu.mapapi.search.poi.PoiNearbySearchOption;
import com.baidu.mapapi.search.poi.PoiResult;
import com.baidu.mapapi.search.poi.PoiSearch;
import com.baidu.mapapi.search.route.BikingRouteResult;
import com.baidu.mapapi.search.route.DrivingRouteResult;
import com.baidu.mapapi.search.route.IndoorRouteResult;
import com.baidu.mapapi.search.route.MassTransitRouteResult;
import com.baidu.mapapi.search.route.OnGetRoutePlanResultListener;
import com.baidu.mapapi.search.route.PlanNode;
import com.baidu.mapapi.search.route.RoutePlanSearch;
import com.baidu.mapapi.search.route.TransitRouteResult;
import com.baidu.mapapi.search.route.WalkingRouteLine;
import com.baidu.mapapi.search.route.WalkingRoutePlanOption;
import com.baidu.mapapi.search.route.WalkingRouteResult;
import com.baidu.mapapi.search.sug.OnGetSuggestionResultListener;
import com.baidu.mapapi.search.sug.SuggestionResult;
import com.baidu.mapapi.search.sug.SuggestionSearch;
import com.baidu.mapapi.search.sug.SuggestionSearchOption;
import com.baidu.mapapi.walknavi.WalkNavigateHelper;
import com.baidu.mapapi.walknavi.adapter.IWEngineInitListener;

import org.tensorflow.lite.Interpreter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class MainActivity extends Activity {
    private MapView mMapView = null;
    private BaiduMap mBaiduMap = null;
    private LocationClient mLocationClient = null;
    private BaiduMap.OnMarkerClickListener onMarkerClickListener = null;
    private RoutePlanSearch mSearch = null;
    private LatLng mLocation = null;
    private String mLocationCity = null;
    private PoiSearch mPoiSearch = null;
    private SuggestionSearch mSuggestionSearch = null;
    private static final int BAIDU_LOCATION_PERMISSION = 100;
    private EditText search_box;
    private TextView show_travel_info_view;
    private TextView show_speed_info_view;
    private Interpreter prediction_model = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        load_prediction_model("test0");

        setContentView(R.layout.activity_main);
        //获取地图控件引用
        search_box = findViewById(R.id.editText);
        search_box.addTextChangedListener(new EditChangedListener());
        show_travel_info_view = findViewById(R.id.travelInfo);
        show_speed_info_view = findViewById(R.id.speedInfo);
        mMapView = (MapView) findViewById(R.id.bmapView);
        mBaiduMap = mMapView.getMap();
        mBaiduMap.setMyLocationEnabled(true);
        mSearch = RoutePlanSearch.newInstance();
        mSearch.setOnGetRoutePlanResultListener(routePlanResultListener);
        mSuggestionSearch = SuggestionSearch.newInstance();
        mSuggestionSearch.setOnGetSuggestionResultListener(suggestionResultListener);
        mPoiSearch = PoiSearch.newInstance();
        mPoiSearch.setOnGetPoiSearchResultListener(poiSearchResultListener);

        // 设置地图中心点为辽宁沈阳东北大学浑南校区五舍
        mBaiduMap.setMapStatus(MapStatusUpdateFactory.newLatLng(new LatLng(41.6577396168, 123.4343104372)));
        // 调整缩放级别
        mBaiduMap.setMapStatus(MapStatusUpdateFactory.zoomTo(18));
        Toast.makeText(getApplicationContext(), "正在定位", Toast.LENGTH_SHORT).show();

        initLocationPermission();
        initClick();
        initNavigate();
    }

    private void load_prediction_model(String model) {
        try {
            AssetFileDescriptor fileDescriptor = this.getAssets().openFd(model + ".tflite");
            FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
            FileChannel fileChannel = inputStream.getChannel();
            long startOffset = fileDescriptor.getStartOffset();
            long declaredLength = fileDescriptor.getDeclaredLength();

            this.prediction_model = new Interpreter(fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength));
            Log.d("预测模型", "导入成功");
        } catch (Exception e) {
            Toast.makeText(MainActivity.this, model + "预测模型载入失败", Toast.LENGTH_SHORT).show();
            Log.d("预测模型", "导入失败 " + e.toString());
        }
    }

    // 申请定位所需权限
    private void initLocationPermission() {
        if (getApplicationContext().checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED ||
                getApplicationContext().checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                getApplicationContext().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                getApplicationContext().checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                getApplicationContext().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // 申请一个（或多个）权限，并提供用于回调返回的获取码（用户定义）
            requestPermissions(new String[]{Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE}, BAIDU_LOCATION_PERMISSION);
        } else initLocation();
    }

    // 申请权限的回调函数
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            //requestCode即所声明的权限获取码，在checkSelfPermission时传入
            case BAIDU_LOCATION_PERMISSION:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //获取到权限，做相应处理
                    //调用定位SDK应确保相关权限均被授权，否则会引起定位失败
                    initLocation();
                } else {
                    //没有获取到权限，做特殊处理
                    Toast.makeText(getApplicationContext(), "获取位置权限失败", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                break;
        }
    }

    // 定位初始化
    private void initLocation() {
        LocationManager locManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (!locManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            // 未打开位置开关，可能导致定位失败或定位不准，提示用户或做相应处理
            Toast.makeText(getApplicationContext(), "可以打开位置服务，以获得更精确的定位", Toast.LENGTH_SHORT).show();
        }
        //定位初始化
        mLocationClient = new LocationClient(this);

        //通过LocationClientOption设置LocationClient相关参数
        LocationClientOption option = new LocationClientOption();
        option.setOpenGps(true); // 打开gps
        option.setCoorType("bd09ll"); // 设置坐标类型
        option.setScanSpan(1000);

        //设置locationClientOption
        mLocationClient.setLocOption(option);

        //注册LocationListener监听器
        MyLocationListener myLocationListener = new MyLocationListener();
        mLocationClient.registerLocationListener(myLocationListener);
        //开启地图定位图层
        mLocationClient.start();
    }

    // 设置单击事件
    private void initClick() {
        BaiduMap.OnMapClickListener onMapClickListener = new BaiduMap.OnMapClickListener() {
            /**
             * 地图单击事件回调函数
             *
             * @param point 点击的地理坐标
             */
            @Override
            public void onMapClick(LatLng point) {
            }

            /**
             * 地图内 Poi 单击事件回调函数
             *
             * @param mapPoi 点击的 poi 信息
             */
            @Override
            public boolean onMapPoiClick(MapPoi mapPoi) {
                showSearchButton(mapPoi.getPosition());
                return true;
            }
        };
        //设置地图单击事件监听
        mBaiduMap.setOnMapClickListener(onMapClickListener);

        onMarkerClickListener = new BaiduMap.OnMarkerClickListener() {
            /**
             * 地图 Marker 覆盖物点击事件监听函数
             * @param marker 被点击的 marker
             */
            @Override
            public boolean onMarkerClick(Marker marker) {
                showSearchButton(marker.getPosition());
                return true;//是否捕获点击事件
            }
        };

        // 设置地图 Marker 覆盖物点击事件监听者,自3.4.0版本起可设置多个监听对象，停止监听时调用removeMarkerClickListener移除监听对象
        mBaiduMap.setOnMarkerClickListener(onMarkerClickListener);
    }

    // 点击market之后的显示按钮
    private void showSearchButton(final LatLng location) {
        // 每次点击market，均显示从定位点到该点的路径规划
        mBaiduMap.clear();
        show_travel_info_view.setText("");

        PlanNode stNode;
        if (mLocation != null) stNode = PlanNode.withLocation(mLocation);
        else stNode = PlanNode.withLocation(mLocation);
        PlanNode enNode = PlanNode.withLocation(location);
        mSearch.walkingSearch((new WalkingRoutePlanOption())
                .from(stNode)
                .to(enNode));

//        // 点击market同时会显示Button，点击后进入导航活动
//        // 用来构造InfoWindow的Button
//        Button button = new Button(getApplicationContext());
//        button.setText("导航");
//
//        // 构造InfoWindow
//        //-10 InfoWindow相对于point在y轴的偏移量
//        InfoWindow mInfoWindow = new InfoWindow(button, location, -10);
//
//        // 使InfoWindow生效
//        mBaiduMap.showInfoWindow(mInfoWindow);
//
//        // 按钮点击监听
//        button.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                //起终点位置
//                LatLng startPt = mLocation;
//                LatLng endPt = location;
//                //发起算路
//                WalkNavigateHelper.getInstance().routePlanWithParams(new WalkNaviLaunchParam().stPt(startPt).endPt(endPt), new IWRoutePlanListener() {
//                    @Override
//                    public void onRoutePlanStart() {
//                        //开始算路的回调
//                    }
//
//                    @Override
//                    public void onRoutePlanSuccess() {
//                        //算路成功
//                        //跳转至诱导页面
//                        Intent intent = new Intent(MainActivity.this, NavigationActivity.class);
//                        startActivity(intent);
//                    }
//
//                    @Override
//                    public void onRoutePlanFail(WalkRoutePlanError walkRoutePlanError) {
//                        //算路失败的回调
//                    }
//                });
//            }
//        });
    }

    private void initNavigate() {
        // 获取导航控制类
        // 引擎初始化
        WalkNavigateHelper.getInstance().initNaviEngine(this, new IWEngineInitListener() {

            @Override
            public void engineInitSuccess() {
                //引擎初始化成功的回调
                Toast.makeText(getApplicationContext(), "导航初始化", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void engineInitFail() {
                //引擎初始化失败的回调
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        mLocationClient.restart();
        //在activity执行onResume时执行mMapView. onResume ()，实现地图生命周期管理
        mMapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //在activity执行onPause时执行mMapView. onPause ()，实现地图生命周期管理
        mMapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        prediction_model.close();
        //在activity执行onDestroy时执行mMapView.onDestroy()，实现地图生命周期管理
        mLocationClient.stop();
        mSearch.destroy();
        mSuggestionSearch.destroy();
        mPoiSearch.destroy();
        mBaiduMap.setMyLocationEnabled(false);
        //停止监听时移除监听对象
        mBaiduMap.removeMarkerClickListener(onMarkerClickListener);
        mMapView.onDestroy();
        mMapView = null;
    }

    // 记录路径信息
    public static void writeRecord(Context context, String filename, String content) throws IOException {

        //获取外部存储卡的可用状态
        String storageState = Environment.getExternalStorageState();

        //判断是否存在可用的的SD Card
        if (storageState.equals(Environment.MEDIA_MOUNTED)) {

            //路径： /storage/emulated/0/Android/data/com.yoryky.demo/cache/yoryky.txt
            filename = context.getExternalCacheDir().getAbsolutePath() + File.separator + filename;

            FileOutputStream outputStream = new FileOutputStream(filename, true);
            outputStream.write(content.getBytes());
            outputStream.close();
        }
    }

    // 监听器们
    // 定位监听器
    public class MyLocationListener extends BDAbstractLocationListener {
        @Override
        public void onReceiveLocation(BDLocation location) {
            //mapView 销毁后不在处理新接收的位置
            if (location == null || mMapView == null) {
                return;
            }
            double mLatitude = location.getLatitude();
            double mLongitude = location.getLongitude();
            float mRadius = location.getRadius();
            if (mLatitude != Double.MIN_VALUE && mLongitude != Double.MIN_VALUE) {
                if (prediction_model != null) {
                    float[][] input = {{location.getSpeed()}}, output = {{0}};
                    prediction_model.run(input, output);
                    show_speed_info_view.setText(String.format("当前速度为%s 模型预测的下一步的速度为:%s", String.valueOf(location.getSpeed()), String.valueOf(output[0][0] + " m/s")));
                }
                mLocationCity = location.getCity();
            } else {
                Toast.makeText(getApplicationContext(), "定位失败，使用默认位置 " + location.getLocType(), Toast.LENGTH_SHORT).show();
                mRadius = 0;
                mLatitude = 41.6577396168;
                mLongitude = 123.4343104372;
            }
            mLocation = new LatLng(mLatitude, mLongitude);

            //  记录路径信息
            try {
                String time = DateFormat.format("MM-dd hh:mm:ss", Calendar.getInstance().getTime()).toString();
                String record;
                writeRecord(getApplicationContext(), "test.txt", time + String.valueOf(location.getSpeed()));
            } catch (IOException e) {
                Log.d("路径记录", e.getMessage());
            }

            MyLocationData locData = new MyLocationData.Builder()
                    .accuracy(mRadius)
                    // 此处设置开发者获取到的方向信息，顺时针0-360
                    .direction(location.getDirection()).latitude(mLatitude)
                    .longitude(mLongitude).build();
            mBaiduMap.setMyLocationData(locData);
        }
    }

    // 路线规划监听
    OnGetRoutePlanResultListener routePlanResultListener = new OnGetRoutePlanResultListener() {
        @Override
        public void onGetWalkingRouteResult(WalkingRouteResult walkingRouteResult) {
            if (walkingRouteResult == null) {
                return;
            } else if (walkingRouteResult.error != SearchResult.ERRORNO.NO_ERROR) {
                Toast.makeText(getApplicationContext(), "路线查找失败 " + walkingRouteResult.error,
                        Toast.LENGTH_SHORT).show();
                return;
            }

            //创建WalkingRouteOverlay实例
            WalkingRouteOverlay overlay = new WalkingRouteOverlay(mBaiduMap);
            if (walkingRouteResult.getRouteLines().size() > 0) {
                //获取路径规划数据,(以返回的第一条数据为例)
                //为WalkingRouteOverlay实例设置路径数据
                WalkingRouteLine line = walkingRouteResult.getRouteLines().get(0);
                overlay.setData(line);
                show_travel_info_view.setText("路程距离：" + line.getDistance() + "m 路程时间：" + line.getDuration() + 's');
                //在地图上绘制WalkingRouteOverlay
                overlay.addToMap();
            }
        }

        @Override
        public void onGetBikingRouteResult(BikingRouteResult bikingRouteResult) {
            //创建BikingRouteOverlay实例
            BikingRouteOverlay overlay = new BikingRouteOverlay(mBaiduMap);
            if (bikingRouteResult.getRouteLines().size() > 0) {
                //获取路径规划数据,(以返回的第一条路线为例）
                //为BikingRouteOverlay实例设置数据
                overlay.setData(bikingRouteResult.getRouteLines().get(0));
                //在地图上绘制BikingRouteOverlay
                overlay.addToMap();
            }
        }

        @Override
        public void onGetIndoorRouteResult(IndoorRouteResult indoorRouteResult) {
            //创建IndoorRouteOverlay实例
            IndoorRouteOverlay overlay = new IndoorRouteOverlay(mBaiduMap);
            if (indoorRouteResult.getRouteLines() != null && indoorRouteResult.getRouteLines().size() > 0) {
                //获取室内路径规划数据（以返回的第一条路线为例）
                //为IndoorRouteOverlay实例设置数据
                overlay.setData(indoorRouteResult.getRouteLines().get(0));
                //在地图上绘制IndoorRouteOverlay
                overlay.addToMap();
            }
        }

        @Override
        public void onGetDrivingRouteResult(DrivingRouteResult drivingRouteResult) {
            //创建DrivingRouteOverlay实例
            DrivingRouteOverlay overlay = new DrivingRouteOverlay(mBaiduMap);
            if (drivingRouteResult.getRouteLines().size() > 0) {
                //获取路径规划数据,(以返回的第一条路线为例）
                //为DrivingRouteOverlay实例设置数据
                overlay.setData(drivingRouteResult.getRouteLines().get(0));
                //在地图上绘制DrivingRouteOverlay
                overlay.addToMap();
            }
        }

        @Override
        public void onGetMassTransitRouteResult(MassTransitRouteResult massTransitRouteResult) {
            //创建MassTransitRouteOverlay实例
            MassTransitRouteOverlay overlay = new MassTransitRouteOverlay(mBaiduMap);
            if (massTransitRouteResult.getRouteLines() != null && massTransitRouteResult.getRouteLines().size() > 0) {
                //获取路线规划数据（以返回的第一条数据为例）
                //为MassTransitRouteOverlay设置数据
                overlay.setData(massTransitRouteResult.getRouteLines().get(0));
                //在地图上绘制Overlay
                overlay.addToMap();
            }
        }

        @Override
        public void onGetTransitRouteResult(TransitRouteResult transitRouteResult) {
            //创建TransitRouteOverlay实例
            TransitRouteOverlay overlay = new TransitRouteOverlay(mBaiduMap);
            //获取路径规划数据,(以返回的第一条数据为例)
            //为TransitRouteOverlay实例设置路径数据
            if (transitRouteResult.getRouteLines().size() > 0) {
                overlay.setData(transitRouteResult.getRouteLines().get(0));
                //在地图上绘制TransitRouteOverlay
                overlay.addToMap();
            }
        }
    };

    // 输入框监听
    class EditChangedListener implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            String city;
            if (mLocationCity == null) city = "沈阳";
            else city = mLocationCity;
            mSuggestionSearch.requestSuggestion(new SuggestionSearchOption()
                    .city(city)
                    .keyword(search_box.getText().toString()));
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    }


    // 搜索关键词监听
    OnGetSuggestionResultListener suggestionResultListener = new OnGetSuggestionResultListener() {
        @Override
        public void onGetSuggestionResult(final SuggestionResult suggestionResult) {
            if (suggestionResult == null || suggestionResult.getAllSuggestions() == null) {
                return;
            }
            final List<String> suggest = new ArrayList<>();
            List<SuggestionResult.SuggestionInfo> all_suggestion = suggestionResult.getAllSuggestions();
            if (all_suggestion.size() > 5) {
                all_suggestion = all_suggestion.subList(0, 5);
            }
            for (SuggestionResult.SuggestionInfo suggestionInfo : all_suggestion) {
                if (suggestionInfo.key != null) {
                    suggest.add(suggestionInfo.key);
                }
            }
            ArrayAdapter adapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_dropdown_item_1line, suggest);
            final ListView autoCompleteTextView = findViewById(R.id.suggestList);
            autoCompleteTextView.setAdapter(adapter);
            // listview监听
            autoCompleteTextView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    autoCompleteTextView.setAdapter(null);
                    LatLng search_location;
                    if (mLocation == null)
                        search_location = new LatLng(41.6577396168, 123.4343104372);
                    else
                        search_location = new LatLng(mLocation.latitude, mLocation.longitude);
                    String info = suggest.get(position);
                    mPoiSearch.searchNearby(new PoiNearbySearchOption()
                            .keyword(info)
                            .location(search_location)
                            .radius(5000));
                }
            });
            adapter.notifyDataSetChanged();
        }
    };

    // poi搜索监听
    OnGetPoiSearchResultListener poiSearchResultListener = new OnGetPoiSearchResultListener() {
        @Override
        public void onGetPoiResult(PoiResult poiResult) {
            if (poiResult.error == SearchResult.ERRORNO.RESULT_NOT_FOUND) {
                Toast.makeText(getApplicationContext(), "未找到结果", Toast.LENGTH_LONG).show();
                return;
            }
            if (poiResult.error != SearchResult.ERRORNO.NO_ERROR) {
                Toast.makeText(getApplicationContext(), "搜索错误" + poiResult.error, Toast.LENGTH_LONG).show();
                return;
            }
            //搜索到POI
            mBaiduMap.clear();
            StringBuilder icon_name = new StringBuilder("Icon_mark0.png");
            int tem = 1;
            for (PoiInfo one_poi : poiResult.getAllPoi()) {
                icon_name.replace(9, 10, Integer.toString(tem));
                mBaiduMap.addOverlay(
                        new MarkerOptions()
                                .position(one_poi.location)
                                .icon(BitmapDescriptorFactory.fromAsset(icon_name.toString())));
                tem++;
            }
        }

        @Override
        public void onGetPoiDetailResult(PoiDetailSearchResult poiDetailSearchResult) {

        }

        @Override
        public void onGetPoiIndoorResult(PoiIndoorResult poiIndoorResult) {
        }

        //废弃
        @Override
        public void onGetPoiDetailResult(PoiDetailResult poiDetailResult) {
        }
    };
}