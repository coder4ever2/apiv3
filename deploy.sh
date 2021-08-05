#!/bin/sh
gcloud builds submit --tag gcr.io/mimio-es-wmtr/apiv3
gcloud run deploy apiv3 --image gcr.io/mimio-es-wmtr/apiv3 

