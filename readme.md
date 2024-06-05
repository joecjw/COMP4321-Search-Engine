# Final Phase Program Specification

## File Structure

After unzipping the folder, You should see the following:

- This `readme.md` file

- A `ui-guide-images` folder containing images for the user manual section below

- A `Final Phase Report.pdf` Documentation

- The Final Program Directory called `search-engine`

## Installation Procedures

To run the program, you need the following things installed:

- **Recommended System**

  - Windows 11 Home (we have tested on this only, so use the same system if possible)

- **Java JDK version 17**

  - Download it [Here](https://www.oracle.com/hk/java/technologies/downloads/#java17)
  - Follow [Here](https://docs.oracle.com/en/java/javase/17/install/overview-jdk-installation.html#GUID-8677A77F-231A-40F7-98B9-1FD0B48C346A)
    to install it
  - To verify the installation, run `java -version` in the Commond Prompt / Shell to get installed version information.

- **Maven version 3.5+ (Install Java JDK before installing Maven)**

  - Download it [Here](https://maven.apache.org/download.cgi)
  - Follow [Here](https://maven.apache.org/install.html) to install it
  - To verify the installation, run `mvn -version` in the Commond Prompt / Shell to get installed version information.

- **Node.js (v20.12.2 LTS) and npm (10.5.2)**

  - Download it [Here](https://nodejs.org/en/download)
  - To verify the installation, run `node -v` and `npm -v` in the Commond Prompt / Shell to get installed version information.

- **Install frontend dependencies (install Node.js and npm first)**
  - Go to frontend directory in Commond Prompt / Shell using `cd <extracted_folder_path>/search-engine/search-engine-frontend`
  - Install dependencies using `npm install`
  - After installation, you should see a new folder called `node_modules` containing all required dependencies.

## Program Description

- The project backend is a springboot maven application runs
  on port 8080 on localhost by default.

- The project frontend is a react application runs on port 5173 on localhost by default.

## User Manual

- ### To run the frontend application

  - #### Open Commond Prompt / Shell and `cd` into the project directory using  
    `cd <extracted_folder_path>/search-engine/search-engine-frontend`

  - #### Run commond `npm run dev`, you should see the server running and generating logs.

  - #### Open the webpage link in a browser to access the UI.

- ### To run the backend application

  - #### Open Commond Prompt / Shell and `cd` into the project directory where the `pom.xml` file is located using  
    `cd <extracted_folder_path>/search-engine/search-engine-backend/search-engine-backend`

  - #### Run commond `mvn spring-boot:start`, you should see the server running and generating logs.

- ### How to use the Frontend UI to crawl and search

  - #### Run the Frontend UI and Backend Server, Open the  generated webpage link in a browser to access the UI. You should see the following inital page.
    ![](/ui-guide-images/init.png)

  - #### Go to the Crawler Panel and `Click` on the `Crawl` button to crawl webpages before searching. (`Note:` The Root URL and Max Number of webpages to crawl have been set default to the project requirment)
    ![](/ui-guide-images/crawl.png)
  
  - #### Go back to the Search Panel, you should see the loading animation of keyword list and a list of stemmed keywords will be shown.
    ![](/ui-guide-images/crawl_after.png)
  
  - #### Search with Stemmed Keywords List
    - `Click` on the keywords and they will be append to the `Search Bar`. Then `Click` on the `SEARCH` button to issue a search request. A list of retrieved pages will be displayed in a short time. (`Note:` You can only search with either stemmed keywords or unstemmed keywords, `DO NOT`  mix any unstemmed keywords with the stemmed keywords in a query.)
    ![](/ui-guide-images/stemmed_keyword_search.png)

  - #### Simple Keywords Search
    - Type any keywords in the `Search Bar`. Then `Click` on the `SEARCH` button to issue a search request. A list of retrieved pages will be displayed in a short time.
    ![](/ui-guide-images/simple_keyword.png)

  - #### Phrase Search
    - Type any phrase in the `Search Bar`. `Enclose` the phrase with double quotes `"<your search phrase>"`. Then `Click` on the `SEARCH` button to issue a search request. A list of retrieved   pages will be displayed in a short time.
    ![](/ui-guide-images/phrase_1.png)
    ![](/ui-guide-images/phrase_2.png)

  - #### Customization on Target Section of Searching
    - Simple Keywords Search on `Title` ONLY
    ![](/ui-guide-images/simple_title_1.png)
    ![](/ui-guide-images/simple_title_2.png)

    - Simple Keywords Search on `Body` ONLY
    ![](/ui-guide-images/simple_body.png)

    - Phrase Search on `Title` ONLY
    ![](/ui-guide-images/phrase_title_1.png)
    ![](/ui-guide-images/phrase_title_2.png)

    - Phrase Search on `Body` ONLY
    ![](/ui-guide-images/phrase_body_1.png)
    ![](/ui-guide-images/phrase_body_2.png)
    ![](/ui-guide-images/phrase_body_3.png)
  
  - #### Relevance Feedback (Get Similar Pages)
    - The following shows a random Simple Keywords Search results
    ![](/ui-guide-images/similar_page_before.png)

    - `Click` on the `GET SIMILAR PAGES` button of the first result page(UG). Then the top5 frequent stemmed keywords will be concatenated and put into the `Search Bar` as shown below. (`Note:` After the newly formed query is put into the `Search Bar`, you could still choose your desired search section for customization)
    ![](/ui-guide-images/similar_page_query_formation.png)

    - `Click` on the `SEARCH` button to search with the newly formed query. Observe that the retrieved results have higher simliarity scores and contains more similar pages.
    ![](/ui-guide-images/similar_page_after.png)


- ### To stop the frontend application

  - #### In the opened Commond Prompt / Shell for frontend application, Press `Ctrl + C` and Enter `y` to confirm ternination of the application.

- ### To stop the backend application

  - #### In the opened Commond Prompt / Shell for backend application, Run commond `mvn spring-boot:stop`you should see logs indicating the server is stopped.
