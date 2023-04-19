package src.main.java.com.shinear;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.gson.Gson;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class Test {
    private static final String API_URL = "https://example.com/riskvolatility";
    private static final String[] PID_ARRAY = new String[1008]; // 假设 PID 数组已经填充好了

    public static void main(String[] args) throws InterruptedException {
        OkHttpClient client = new OkHttpClient();
        Gson gson = new Gson();

        ExecutorService executorService = Executors.newFixedThreadPool(10); // 创建一个线程池，最多同时执行 10 个线程
        CompletionService<String> completionService = new ExecutorCompletionService<>(executorService);

        List<Callable<String>> tasks = new ArrayList<>();
        for (int i = 0; i < PID_ARRAY.length; i += 100) {
            List<String> subList = Arrays.asList(PID_ARRAY).subList(i, Math.min(i + 100, PID_ARRAY.length));
            tasks.add(new Callable<String>() {
                @Override
                public String call() throws Exception {
                    String pidString = String.join(",", subList);
                    Request request = new Request.Builder()
                            .url(API_URL)
                            .header("qqq", "12")
                            .header("ttt", "123")
                            .header("uuu", "980")
                            .post(RequestBody.create(MediaType.parse("application/json"), pidString))
                            .build();
                    try (Response response = client.newCall(request).execute()) {
                        if (response.isSuccessful()) {
                            return response.body().string();
                        } else {
                            return null;
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        return null;
                    }
                }
            });
        }

        for (Callable<String> task : tasks) {
            completionService.submit(task);
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tasks.size(); i++) {
            String result = completionService.take().get();
            if (result != null) {
                sb.append(result);
            }
        }

        String[] resultArray = gson.fromJson(sb.toString(), String[].class);
        List<String> resultList = Arrays.asList(resultArray);
        System.out.println(resultList);
    }
}
