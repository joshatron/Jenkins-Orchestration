package io.joshatron.jenkins.operations;

import io.joshatron.jenkins.config.JenkinsConfig;
import io.joshatron.jenkins.config.JenkinsJob;
import io.joshatron.jenkins.config.JenkinsServer;
import io.joshatron.jenkins.config.Parameter;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Builder {

    public Builder() {
        throw new IllegalStateException("This is a utility class");
    }

    public static void buildJob(JenkinsServer server, String jobName, Arguments arguments) {
        try(CloseableHttpClient client = HttpClients.createDefault()) {

            JenkinsJob job = server.getJob(jobName);
            String url = server.getBaseUrl() + job.getUrl() + "/build";
            String payload = createPayload(job, arguments);
            ArrayList<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("json", payload));

            HttpPost request = new HttpPost(url);
            request.setHeader("Authorization", server.getAuth());
            request.setEntity(new UrlEncodedFormEntity(params));

            HttpResponse response = client.execute(request);
            if(response.getStatusLine().getStatusCode() == HttpStatus.SC_CREATED) {
                System.out.println("Build triggered for " + job.getName());
            }
            else {
                System.out.println("Build unsuccessful: " + response.getStatusLine().getStatusCode());
                System.out.println(EntityUtils.toString(response.getEntity()));
            }
        } catch (Exception e) {
            System.out.println("Exception while building job");
            e.printStackTrace();
        }
    }

    private static String createPayload(JenkinsJob job, Arguments arguments) {
        JSONObject toReturn = new JSONObject();

        JSONArray parameters = new JSONArray();
        for(Parameter parameter : job.getParameters()) {
            JSONObject p = new JSONObject();
            p.put("name", parameter.getArgName());
            if(arguments.hasArgument(parameter.getName())) {
                p.put("value", arguments.getArgs().get(parameter.getName()));
            }
            else if(parameter.getDefaultValue() != null && !parameter.getDefaultValue().isEmpty()) {
                p.put("value", parameter.getDefaultValue());
            }
            else {
                p.put("value", "");
            }
            parameters.put(p);
        }
        toReturn.put("parameter", parameters);

        return toReturn.toString();
    }

    public static void buildJobs(JenkinsConfig config, Arguments arguments) throws IOException {
        for(JenkinsServer server : config.getServers()) {
            for(JenkinsJob job : server.getJobs()) {
                buildJob(server, job.getName(), arguments);
            }
        }
    }

    public static void buildJobs(JenkinsConfig config, List<String> tags, Arguments arguments) throws IOException {
        buildJobs(config.getJobsWithTags(tags), arguments);
    }

    public static void buildJobs(JenkinsConfig config, String tag, Arguments arguments) throws IOException {
        buildJobs(config.getJobsWithTag(tag), arguments);
    }
}
