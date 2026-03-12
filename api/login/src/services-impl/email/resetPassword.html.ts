export const mailResetPassHTML = (name: string, confirmLink: string) => `<!doctype html>
<html lang="en-US">
<head>
  <link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link href="https://fonts.googleapis.com/css2?family=Poppins:ital,wght@0,100;0,200;0,300;0,400;0,500;0,600;0,700;0,800;0,900;1,100;1,200;1,300;1,400;1,500;1,600;1,700;1,800;1,900&display=swap" rel="stylesheet">
  
  <meta content="text/html; charset=utf-8" http-equiv="Content-Type" />
  <title>Reset Password</title>
  <style type="text/css">
    a:hover {
      text-decoration: underline !important;
    }
    *{
      font-family: "Arial", sans-serif;
    }
  </style>
</head>

<body marginheight="0" topmargin="0" marginwidth="0" style="margin: 0px; background-color: #f2f3f8;" leftmargin="0">
  <!--100% body table-->
  <table cellspacing="0" border="0" cellpadding="0" width="100%" bgcolor="#f2f3f8">
    <tr>
      <td>
        <table style="background-color: #f2f3f8; max-width:670px;  margin:0 auto;" width="100%" border="0" align="center" cellpadding="0" cellspacing="0">
          <tr>
            <td style="height:80px;">&nbsp;</td>
          </tr>
          <tr>
            <td style="text-align:center;">
              <a href="https://hira.one/" title="logo" target="_blank">
                <img width="260"  src="https://senspark.com/static/media/logo.063a22b07e3760cdf4d5.png" title="logo" alt="logo">
              </a>
            </td>
          </tr>
          <tr>
            <td style="height:20px;">&nbsp;</td>
          </tr>
          <tr>
            <td>
              <table width="95%" border="0" align="center" cellpadding="0" cellspacing="0" style="max-width:670px;background:#fff; border-radius:3px; text-align:left;-webkit-box-shadow:0 6px 18px 0 rgba(0,0,0,.06);-moz-box-shadow:0 6px 18px 0 rgba(0,0,0,.06);box-shadow:0 6px 18px 0 rgba(0,0,0,.06);">
                <tr>
                  <td style="height:40px;">&nbsp;</td>
                </tr>
                <tr>
                  <td style="padding:0 35px;">
                    <h1 style="color:#063e5d; font-weight:500; margin:0;font-size:32px;font-family:'Poppins',sans-serif; text-align:center;">Reset Password
                     <span style="display:block; margin-left:auto; margin-right:auto; margin-top:19px; margin-bottom:30px; border-bottom:1px solid #cecece; width:100px;"></span>
                    </h1>
                
                    <p style="color:#455056; font-size:17px;line-height:24px; margin:0;font-weight:500;">Hi <span style="font-weight: 800;"> ${name} </span> ,</p>
                    <br/>
                    <p style="color:#455056; font-size:16px;line-height:24px; margin:0;">          
                      There was a request to change your password!

                      If you did not make this request then please ignore this email.

                      Otherwise, please click below to change your password: 
                    </p>
                    
                     <p style="margin-bottom:0px;">
                  Regards,
                </p>
                <p>Team Bomberland</p>
                   
                  </td>
                </tr>
                <tr>
                  <td style="text-align:center;">
                     <a href="${confirmLink}" target="_blank" style="background:#e44525;text-decoration:none !important; font-weight:500; margin-top:35px; color:#fff;text-transform:uppercase; font-size:14px;padding:10px 24px;display:inline-block;border-radius:50px;text-align:center;">Reset
                      Password</a>
                  </td>
                </tr>
                <tr style="text-align: center;">
                  <td style="padding:0 35px;">
                    <p style="font-size:16px">
                     You can click here
                    </p>
                    <a
                       style="cursor: pointer; font-size:15px"
                       target="_blank"
                       href="${confirmLink}"
                     >
                      ${confirmLink}
                    </a>
                  </td>
                </tr>
                <tr>
                  <td style="height:40px;">&nbsp;</td>
                </tr>
              </table>
            </td>
          <tr>
            <td style="height:20px;">&nbsp;</td>
          </tr>
          <tr>
            <td style="height:80px;">&nbsp;</td>
          </tr>
        </table>
      </td>
    </tr>
  </table>
  <!--/100% body table-->
</body>

</html>`;