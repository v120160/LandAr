package hu.hero.landar.net;

import android.content.Context;
import android.util.Log;


import java.util.List;

import hu.hero.landar.MainActivity;
import hu.hero.landar.database.PICDATA;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;

public class GetPicsByDistance{
    private String  mUserId;
    private Context mContext;
    private String  mResponse;

    // 內崁 service
    private interface MyAPIService {
        @GET("/getPicsByDistance")
        Call<List<PICDATA>> getPicsByDistance(@Query("userId") String user,
                                              @Query("lat") double lat,
                                              @Query("lon") double lon,
                                              @Query("distance") double distance );
    }

    public GetPicsByDistance(Context context) {
        mContext = context;
    }

    public void get(String userid , double lat , double lon , double distance ) {
        mUserId = userid;
//        String url = LitePal.findFirst( SETTING.class ).getServerUrl();
        String url = "http://192.168.1.25:8082";

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(url)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        MyAPIService service = retrofit.create(MyAPIService.class);

        Call<List<PICDATA>> call = service.getPicsByDistance(userid, lat, lon, distance );

        // 此段為非同步請求
        call.enqueue(new Callback<List<PICDATA>>() {
            public void onResponse(Call<List<PICDATA>> call, Response<List<PICDATA>> response) {
                // 連線成功
                List<PICDATA> list = response.body();
                Log.d("title", list.toString());
                ((MainActivity)mContext).onReadPicListFinish(list);
            }

            @Override
            public void onFailure(Call<List<PICDATA>> call, Throwable t) {
                // 連線失敗
                Log.d("title", "Fail");
                ((MainActivity)mContext).onReadPicListFinish(null);
            }
        });

    }
}
