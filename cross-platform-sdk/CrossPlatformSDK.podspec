Pod::Spec.new do |s|
  s.name = 'CrossPlatformSDK'
  s.version = '1.0.2-beta2'

  s.summary = 'BoxPay Cross Platform SDK'
  s.homepage = 'https://github.com/BoxPay-SDKs/cross-platform-sdk'

  s.license = { :type => 'MIT' }
  s.authors = { 'BoxPay' => 'ishika.bansal@boxpay.tech' }

  s.source = {
    :http => 'https://github.com/BoxPay-SDKs/cross-platform-sdk/releases/download/1.0.2-beta2/cross_platform_sdk.xcframework.zip'
  }

  s.vendored_frameworks = 'cross_platform_sdk.xcframework'

  s.platform = :ios, '12.0'
  s.static_framework = true
end