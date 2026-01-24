package pt.ipt.ddam2025.fittrack.api

import com.google.gson.Gson
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class API {

    companion object {
        // Function to handle all HTTP methods
        fun callApi(apiUrl: String, token: String, httpMethod: String, requestModel: Any? = null): String {
            val response = StringBuilder()
            try {
                val url = URL(apiUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = httpMethod // Set the HTTP method (GET, POST, PUT, DELETE)

                // Set request headers for JSON format and authorization
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Accept", "application/json")
                connection.setRequestProperty("Authorization", "Bearer $token")

                // Send request body for POST/PUT methods
                if (httpMethod == "POST" || httpMethod == "PUT") {
                    connection.doOutput = true
                    requestModel?.let {
                        val jsonInput = Gson().toJson(it)
                        OutputStreamWriter(connection.outputStream).use { os ->
                            os.write(jsonInput)
                            os.flush()
                        }
                    }
                }

                // Handle the response
                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
                    BufferedReader(InputStreamReader(connection.inputStream, "utf-8")).use { br ->
                        var responseLine: String?
                        while (br.readLine().also { responseLine = it } != null) {
                            response.append(responseLine?.trim())
                        }
                    }
                } else {
                    // Handle error response
                    BufferedReader(InputStreamReader(connection.errorStream, "utf-8")).use { br ->
                        var responseLine: String?
                        while (br.readLine().also { responseLine = it } != null) {
                            response.append(responseLine?.trim())
                        }
                    }
                    println("Error Response Code: $responseCode, Message: ${connection.responseMessage}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                return e.message.toString()
            }


            return response.toString()
        }
    }
}