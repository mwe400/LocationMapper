#NMRP project - Spatial Data Analysis with Google's Geocoding API

#load libraries
library(dplyr)
library(stringr)
library(RJSONIO)
library(RCurl)
library(ggplot2)
library(ggmap)
library(mapview)   
library(sp)

#load the csv file into the workspace
geo1 <- read.csv(file.choose(), header = TRUE, sep =",")

#some data cleaning
Matt1 <- geo1
str(Matt1)
Matt1 <- Matt1[, -1]

#changing the data types
Matt1$County <- as.character(Matt1$County)
Matt1$Location <- as.character(Matt1$Location)
str(Matt1)
summary(Matt1)

#consolidating the records based on County
dataMatt <- Matt1 %>%
            select(County,Location,Frequency) %>%
            group_by(County, Location) %>%
            summarise(Frequency = sum(Frequency))

str(dataMatt)

#create a new varaible for the count of words in the location variable
dataMatt$word <-  vapply(strsplit(dataMatt$Location, "\\W+"), length, integer(1))

#Omit the locations which are having the word count higher than 10
dataMatt <- subset(dataMatt ,dataMatt$word < 6, select =  c(1:3))

geo1 <- dataMatt

View(geo1)

#create new columns to call the geocode function
geo1$State <- NA
geo1$lat<-NA
geo1$long<-NA
geo1$formatted_address<-NA

#Create a new column to split the Statename from the county
geo1$State <- as.data.frame(str_split_fixed(as.character(geo1$County), "-", 2))[,2]
geo1$State <- trimws(geo1$State)

#Defining geocode function with the API-KEY
getGeoData <- function(location) {
  location <- gsub(' ','+',location)
  geo_data <- getURL(paste("https://maps.googleapis.com/maps/api/geocode/json?address=",location,"&key=AIzaSyA6FvTYYJ8vPdSse9ElrLWUeQa9pXJAC7o", sep=""))
  #geo_data <- getURL(paste("https://maps.googleapis.com/maps/api/geocode/json?address=",location,sep=""))
  raw_data_2 <- fromJSON(geo_data)
  return(raw_data_2)
}

#loop to generate the geographical coordinates of every record
#Pick the first location coordinates in case of one value
for(i in 1:nrow(geo1))
{
  jsonData <- getGeoData(as.character(geo1$Location[i]))
  #jsonData <- geocode(as.character(geo1$Location[i]),output="all",override_limit=TRUE)
 
  print(geo1$Location[i])
  print(jsonData$status)
  if (jsonData$status == "OK") {
    if (length(jsonData$results) == 1) {
       formattedAddress <- jsonData$results[[1]]$formatted_address
       geo1$formatted_address[i] <- formattedAddress
       #Focusing only on the locations pertaining to USA
       if(grepl("USA", formattedAddress)) {
         geo1$lat[i] <- getElement(jsonData$results[[1]]$geometry$location,"lat")
         geo1$long[i] <- getElement(jsonData$results[[1]]$geometry$location,"lng")
       }
       
       #If there are multiple locations, pick the location coordinates matching with its corresponding state
    } else {
      for(j in 1:length(jsonData$results)) {
        formattedAddress <- jsonData$results[[j]]$formatted_address
        geo1$formatted_address[i] <- formattedAddress
        if(grepl("USA", formattedAddress)) {
          print(formattedAddress)
          if(grepl(paste(", ",as.character(geo1$State[i]),sep=""), formattedAddress)) {
            geo1$lat[i] <- getElement(jsonData$results[[j]]$geometry$location,"lat")
            geo1$long[i] <- getElement(jsonData$results[[j]]$geometry$location,"lng")
          }
        }
        
      }
      
    }
  } 
    
}

#check if the latitude and longitudinal coordinates populated
View(geo1)

#get the USA map
USAMap = ggmap(get_map(location = geocode("United States"), scale= 1, zoom= 4,maptype = "roadmap"),  source = "google")
#USAMap

# Static visualization of the Location with respect to its location occurance frequency
USAMap+
geom_jitter(aes(x=long, y=lat, color = County), data=geo1, position = "jitter", size=geo1$Frequency*0.03, color = "blue", alpha = 0.3) +
scale_size_continuous(range=range(geo1$Frequency)) +geom_text(data = geo1, aes(x = long, y = lat, label = Frequency),
                                                                  size= 2, vjust = 1, hjust = 1, check_overlap = TRUE)



#To Create a interactive map

#omit the NA's records for geographical coordinates
geo1spat <- na.omit(geo1)

#use sp functions and feed the coordinates
coordinates(geo1spat) <- ~long+lat
proj4string(geo1spat) <- "+init=epsg:4326"

# Interactive Map visualization with respect to its frequency of occurance
mapview(geo1spat,  burst =FALSE,use.layer.names = FALSE,color = mapviewGetOption("vector.palette"),
        col.regions = mapviewGetOption("vector.palette"), cex = geo1spat$Frequency*0.04,pch=2)










