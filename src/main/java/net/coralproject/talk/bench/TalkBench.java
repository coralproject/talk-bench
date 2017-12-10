/**
 * 
 */
package net.coralproject.talk.bench;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.common.base.Preconditions;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.thedeanda.lorem.Lorem;
import com.thedeanda.lorem.LoremIpsum;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.Buffer;

/**
 * A script to benchmark Talk from the Coral Project
 * 
 * @author Jeff Nelson
 */
public class TalkBench {

    public static class Options {

        @Parameter(names = { "-u",
                "--root-url" }, description = "The Talk Root URL")
        private String rootUrl;

        @Parameter(names = { "-c",
                "--count" }, description = "The number of comments to post during the benchmark")
        private int count;

        @Parameter(names = { "-e", "--email" }, description = "The user email")
        private String email;

        @Parameter(names = { "-p",
                "--password" }, description = "The user password")
        private String password;

        @Parameter(names = { "-i",
                "--asset-id" }, description = "The asset id of the story to which the comments are posted")
        private String assetId;

        @Parameter(names = {
                "--verbose" }, description = "Log extra information for debugging")
        private boolean verbose = false;
        
        @Parameter(names = {
        "-h", "--help" }, description = "Show the help text")
        private boolean help = false;

    }

    public static void main(String... args) throws IOException, InterruptedException {
        Options options = new Options();
        JCommander parser = JCommander.newBuilder().addObject(options).build();
        parser.parse(args);
        
        if(options.help) {
            parser.usage();
            System.exit(1);
        }

        // Validate the options
        try {
            Preconditions.checkArgument(options.rootUrl != null,
                    "Please specify the root url.");
            Preconditions.checkArgument(options.count > 0,
                    "Please specify a count greater than 0");
            Preconditions.checkArgument(options.email != null,
                    "Please specify a user email address");
            Preconditions.checkArgument(options.password != null,
                    "Please specify a user password");
            Preconditions.checkArgument(options.assetId != null);
            options.rootUrl = options.rootUrl
                    + (options.rootUrl.endsWith("/") ? "" : "/");
        }
        catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            parser.usage();
            System.exit(1);
        }

        OkHttpClient client = new OkHttpClient();
        MediaType mediaType = MediaType.parse("application/json");

        // Perform authentication
        String authUrl = options.rootUrl + "api/v1/auth/local";
        RequestBody body = RequestBody.create(mediaType,
                "{\"email\":\"" + options.email + "\",\"password\":\""
                        + options.password + "\"}");
        Request request = new Request.Builder().url(authUrl).post(body)
                .addHeader("pragma", "no-cache")
                .addHeader("origin", options.rootUrl)
                .addHeader("accept-encoding", "gzip, deflate, br")
                .addHeader("accept-language", "en-US,en;q=0.8")
                .addHeader("user-agent",
                        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_2) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/61.0.3163.100 Safari/537.36")
                .addHeader("content-type", "application/json")
                .addHeader("accept", "application/json")
                .addHeader("cache-control", "no-cache")
                .addHeader("cookie", "_ga=GA1.2.2105589570.1495202636")
                .addHeader("connection", "keep-alive")
                .addHeader("postman-token",
                        "46663940-d46b-9656-98d7-8a51c6260b55")
                .build();
        Response response = client.newCall(request).execute();
        if(response.code() == 200) {
            JsonObject object = new JsonParser().parse(response.body().string())
                    .getAsJsonObject();
            String token = object.get("token").getAsString();
            System.out.println("Authentication was successful!");

            // Start posting comments
            String url = options.rootUrl + "api/v1/graph/ql";
            System.out.println("Performing write benchmark against " + url
                    + " by posting " + options.count + " comments...");
            TimeUnit fromUnit = TimeUnit.NANOSECONDS;
            Lorem lorem = LoremIpsum.getInstance();
            Random random = new Random();
            long totalElapsed = 0;
            for (int i = 0; i < options.count; ++i) {
                String comment = "";
                int seed = random.nextInt();
                if(seed % 6 == 0) {
                    comment = "Fuck you!";
                }
                else if(seed % 2 == 0) {
                    comment = lorem.getWords(10, 30);
                }
                else if(seed % 3 == 0) {
                    comment = lorem.getParagraphs(2, 6);
                }
                else {
                    comment = lorem.getParagraphs(1, 3);
                }
                body = RequestBody.create(mediaType,
                        "{\"query\":\"mutation PostComment($input: CreateCommentInput!) {\\n  createComment(input: $input) {\\n    ...CreateCommentResponse\\n    __typename\\n  }\\n}\\n\\nfragment CreateCommentResponse on CreateCommentResponse {\\n  ...Coral_CreateCommentResponse\\n  ...CoralEmbedStream_CreateCommentResponse\\n  ...TalkMemberSince_CreateCommentResponse\\n  __typename\\n}\\n\\nfragment Coral_CreateCommentResponse on CreateCommentResponse {\\n  errors {\\n    translation_key\\n    __typename\\n  }\\n  __typename\\n}\\n\\nfragment CoralEmbedStream_CreateCommentResponse on CreateCommentResponse {\\n  comment {\\n    ...CoralEmbedStream_CreateCommentResponse_Comment\\n    replies {\\n      nodes {\\n        ...CoralEmbedStream_CreateCommentResponse_Comment\\n        __typename\\n      }\\n      startCursor\\n      endCursor\\n      hasNextPage\\n      __typename\\n    }\\n    __typename\\n  }\\n  actions {\\n    __typename\\n    ... on FlagAction {\\n      reason\\n      message\\n      __typename\\n    }\\n  }\\n  __typename\\n}\\n\\nfragment CoralEmbedStream_CreateCommentResponse_Comment on Comment {\\n  id\\n  body\\n  created_at\\n  status\\n  replyCount\\n  asset {\\n    id\\n    title\\n    url\\n    __typename\\n  }\\n  tags {\\n    tag {\\n      name\\n      created_at\\n      __typename\\n    }\\n    assigned_by {\\n      id\\n      __typename\\n    }\\n    __typename\\n  }\\n  user {\\n    id\\n    username\\n    __typename\\n  }\\n  action_summaries {\\n    count\\n    current_user {\\n      id\\n      created_at\\n      __typename\\n    }\\n    __typename\\n  }\\n  editing {\\n    edited\\n    editableUntil\\n    __typename\\n  }\\n  parent {\\n    id\\n    __typename\\n  }\\n  __typename\\n}\\n\\nfragment TalkMemberSince_CreateCommentResponse on CreateCommentResponse {\\n  comment {\\n    user {\\n      created_at\\n      __typename\\n    }\\n    __typename\\n  }\\n  __typename\\n}\\n\",\"variables\":{\"input\":{\"asset_id\":\""
                                + options.assetId + "\",\"body\":\"" + comment
                                + "\",\"tags\":[]}},\"operationName\":\"PostComment\"}");
                request = new Request.Builder().url(url).post(body)
                        .addHeader("pragma", "no-cache")
                        .addHeader("origin", url)
                        .addHeader("accept-encoding", "gzip, deflate, br")
                        .addHeader("accept-language", "en-US,en;q=0.8")
                        .addHeader("authorization", "Bearer " + token)
                        .addHeader("content-type", "application/json")
                        .addHeader("accept", "*/*")
                        .addHeader("cache-control", "no-cache")
                        .addHeader("user-agent",
                                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_2) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/61.0.3163.100 Safari/537.36")
                        .addHeader("cookie", "_ga=GA1.2.2105589570.1495202636")
                        .addHeader("connection", "keep-alive")
                        .addHeader("postman-token",
                                "2c86c023-a115-fc86-8a2c-ef1e9b0e34d5")
                        .build();
                if(options.verbose) {
                    Buffer buffer = new Buffer();
                    request.body().writeTo(buffer);
                    System.out.println(request.headers());
                    System.out.println(buffer.readUtf8());
                }

                long start = System.nanoTime();
                response = client.newCall(request).execute();
                long elapsed = fromUnit.toMillis(System.nanoTime() - start);
                Thread.sleep(random.nextInt(1000));
                totalElapsed += elapsed;
                System.out.println("Took " + elapsed + " ms");
                System.out.println("Response Code: " + response.code());
                System.out.println(response.headers());
                System.out.println(response.body().string());
                System.out.println("==============================");
            }
             System.out.println("Took "+totalElapsed+" ms to post "+options.count+" comments");       
        }
        else {
            System.err.println("Invalid username/password");
            System.err.println(response.body().string());
            System.exit(1);
        }
    }

}
