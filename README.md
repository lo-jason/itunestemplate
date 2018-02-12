# How to use 

```Usage: java -jar gccapture-1.01-SNAPSHOT.jar <args> --csv=<csv>```

cards****.png will be output in execution directory

## REQUIRED Arguments

```--csv=<csv file>    csv file```

Lower priority but will also work: java -jar gccapture <csv file>

## OPTIONAL Arguments

```--help              this message
--html=<html>       if "ppdg-template" is not in execution directory
--value=<value>     Case-insensitive column name with value of GC, DEFAULT: Amount
--merchant=<value>  Case-insensitive column name with value of GC, DEFAULT: Merchant
                    Note: if your CSV is all iTunes can ignore this
--code=<value>      Case-insensitive column name with code of GC,  DEFAULT: Code
--prefix=<value>    <value>0001.png,  DEFAULT: card
