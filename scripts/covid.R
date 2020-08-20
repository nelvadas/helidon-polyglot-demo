require(lattice);

function(param){
departement<-78
print(param$dep)
print(param$csvFilePath)
departement<-param$dep


svg();
frsdatafile=download.file(url="https://www.data.gouv.fr/fr/datasets/r/6fadff46-9efd-4c53-942a-54aca783c30c", destfile="/tmp/covid-data.csv")
frdata <- read.table(file="/tmp/covid-data.csv" , sep=";", h=TRUE);
names(frdata)
covid_ds = subset(frdata, frdata$dep == departement)
attach(covid_ds)
#X contient les dates
x<-as.Date(jour,format = "%Y-%m-%d")

# y le nombre  d'hospitalisations
y<-incid_hosp

# z le nombre de réanimation
z<-incid_rea
# Courbe des hospitalisations 

g1<-xyplot(y~x,type="l", ylab="Nouvelles Hospitalisations COVID-19",col="blue",main=paste(" D ",departement)); 

print(g1)
grDevices:::svg.off()
}


